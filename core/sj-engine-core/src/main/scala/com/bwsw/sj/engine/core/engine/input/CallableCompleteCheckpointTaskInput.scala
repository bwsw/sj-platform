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

import com.bwsw.common.SerializerInterface
import com.bwsw.sj.common.engine.core.entities._
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory
import scaldi.Injector

/**
  * Class is responsible for handling kafka inputs and t-stream inputs
  *
  * @author Kseniya Mikhaleva
  * @param manager       Manager of environment of task of regular/windowed module
  * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
  *                      which will be retrieved into a module
  */
class CallableCompleteCheckpointTaskInput[T <: AnyRef](manager: CommonTaskManager,
                                                       blockingQueue: WeightedBlockingQueue[EnvelopeInterface],
                                                       override val checkpointGroup: CheckpointGroup,
                                                       envelopeDataSerializer: SerializerInterface)
                                                      (implicit injector: Injector)
  extends CallableCheckpointTaskInput[Envelope](manager.inputs) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val kafkaCheckpointTaskInput = new CallableKafkaCheckpointTaskInput[T](
    manager,
    blockingQueue,
    checkpointGroup)
  private val tStreamCheckpointTaskInput = new CallableTStreamCheckpointTaskInput[T](
    manager,
    blockingQueue,
    checkpointGroup,
    envelopeDataSerializer)

  override def registerEnvelope(envelope: Envelope): Unit = {
    logger.debug(s"Register an envelope: ${envelope.toString}")
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        tStreamCheckpointTaskInput.registerEnvelope(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        kafkaCheckpointTaskInput.registerEnvelope(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined " +
          s"for regular/windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined " +
          s"for regular/windowed streaming engine")
    }
  }

  def call(): Unit = {
    tStreamCheckpointTaskInput.call()
    kafkaCheckpointTaskInput.call()
  }

  override def setConsumerOffsetToLastEnvelope(): Unit = {
    tStreamCheckpointTaskInput.setConsumerOffsetToLastEnvelope()
    kafkaCheckpointTaskInput.setConsumerOffsetToLastEnvelope()
  }

  override def setConsumerOffset(envelope: Envelope): Unit = {
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        tStreamCheckpointTaskInput.setConsumerOffset(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        kafkaCheckpointTaskInput.setConsumerOffset(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined " +
          s"for regular/windowed streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined " +
          s"for regular/windowed streaming engine")
    }
  }

  override def close(): Unit = {
    tStreamCheckpointTaskInput.close()
    kafkaCheckpointTaskInput.close()
  }
}
