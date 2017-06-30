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
package com.bwsw.sj.engine.batch.task.input

import com.bwsw.common.SerializerInterface
import com.bwsw.sj.common.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.input.KafkaTaskInput
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewProducerTransactionPolicy
import org.apache.kafka.clients.consumer.ConsumerRecord
import scaldi.Injector

import scala.collection.JavaConverters._

/**
  * Class is responsible for launching kafka consumers
  * that allow to fetch messages, which are wrapped in envelopes
  * and handle producers to save offsets for further recovering after fails
  *
  * @param manager         allows to manage an environment of batch streaming task
  * @param checkpointGroup group of t-stream agents that have to make a checkpoint at the same time
  * @author Kseniya Mikhaleva
  */
class RetrievableKafkaCheckpointTaskInput[T <: AnyRef](override val manager: CommonTaskManager,
                                                       override val checkpointGroup: CheckpointGroup,
                                                       envelopeDataSerializer: SerializerInterface)
                                                      (override implicit val injector: Injector)
  extends RetrievableCheckpointTaskInput[KafkaEnvelope[T]](manager.inputs) with KafkaTaskInput[T] {
  currentThread.setName(s"batch-task-${manager.taskName}-kafka-consumer")

  override def chooseOffset(): String = {
    val instance = manager.instance.asInstanceOf[BatchInstance]
    instance.startFrom match {
      case EngineLiterals.oldestStartMode => "earliest"
      case _ => "latest"
    }
  }

  override def get(): Iterable[KafkaEnvelope[T]] = {
    logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for" +
      s" $kafkaSubscriberTimeout milliseconds.")
    val records = kafkaConsumer.poll(kafkaSubscriberTimeout)
    records.asScala.map(consumerRecordToEnvelope)
  }

  private def consumerRecordToEnvelope(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]]): KafkaEnvelope[T] = {
    logger.debug(s"Task name: ${manager.taskName}. Convert a consumed kafka record to kafka envelope.")
    val data = envelopeDataSerializer.deserialize(consumerRecord.value()).asInstanceOf[T]
    val envelope = new KafkaEnvelope(data)
    envelope.stream = consumerRecord.topic()
    envelope.partition = consumerRecord.partition()
    envelope.tags = streamNamesToTags(consumerRecord.topic())
    envelope.id = consumerRecord.offset()

    envelope
  }

  override def setConsumerOffsetToLastEnvelope(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Send a transaction containing kafka consumer offsets for all streams.")
    super.setConsumerOffsetToLastEnvelope()
    offsetProducer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }
}