/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.core.engine.input


import com.bwsw.sj.common.engine.core.entities.{EnvelopeInterface, KafkaEnvelope, KafkaRecords, WeightedBlockingQueue}
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.si.model.instance.RegularInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewProducerTransactionPolicy
import scaldi.Injector

/**
  * Class is responsible for launching kafka consumers
  * that put consumed message, which are wrapped in envelope, into a common queue
  * and handling producers to save offsets for further recovering after fails
  *
  * @author Kseniya Mikhaleva
  * @param manager       Manager of environment of task of regular module
  * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
  *                      which will be retrieved into a module
  */
class CallableKafkaCheckpointTaskInput[T <: AnyRef](override val manager: CommonTaskManager,
                                                    blockingQueue: WeightedBlockingQueue[EnvelopeInterface],
                                                    override val checkpointGroup: CheckpointGroup)
                                                   (override implicit val injector: Injector)
  extends CallableCheckpointTaskInput[KafkaEnvelope[T]](manager.inputs) with KafkaTaskInput[T] {
  currentThread.setName(s"regular-task-${manager.taskName}-kafka-consumer")

  def chooseOffset(): String = {
    logger.debug(s"Task: ${manager.taskName}. Get a start-from parameter from instance.")
    val instance = manager.instance.asInstanceOf[RegularInstance]
    instance.startFrom match {
      case EngineLiterals.oldestStartMode => "earliest"
      case _ => "latest"
    }
  }

  override def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run a kafka consumer for regular task in a separate thread of execution service.")

    while (true) {
      logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for " +
        s"$kafkaSubscriberTimeout milliseconds.")
      val records = kafkaConsumer.poll(kafkaSubscriberTimeout)

      if (!records.isEmpty)
        blockingQueue.put(KafkaRecords(records))
    }
  }

  override def setConsumerOffsetToLastEnvelope(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Set a consumer offset to last envelope for all streams.")
    super.setConsumerOffsetToLastEnvelope()
    offsetProducer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }
}