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
import com.bwsw.sj.common.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory
import scaldi.Injector

/**
  * Class is responsible for handling kafka and t-stream input
  * (i.e. retrieving and checkpointing kafka and t-stream messages)
  * for batch streaming engine
  *
  * @param manager allows to manage an environment of batch streaming task
  * @author Kseniya Mikhaleva
  */
class RetrievableCompleteCheckpointTaskInput[T <: AnyRef](manager: CommonTaskManager,
                                                          override val checkpointGroup: CheckpointGroup,
                                                          envelopeDataSerializer: SerializerInterface)
                                                         (implicit injector: Injector)
  extends RetrievableCheckpointTaskInput[Envelope](manager.inputs) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val retrievableKafkaTaskInput = new RetrievableKafkaCheckpointTaskInput[T](
    manager,
    checkpointGroup,
    envelopeDataSerializer)
  private val retrievableTStreamTaskInput = new RetrievableTStreamCheckpointTaskInput[T](
    manager,
    checkpointGroup,
    envelopeDataSerializer)

  override def registerEnvelope(envelope: Envelope): Unit = {
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        retrievableTStreamTaskInput.registerEnvelope(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        retrievableKafkaTaskInput.registerEnvelope(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for batch streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for batch streaming engine")
    }
  }

  override def get(): Iterable[Envelope] = {
    retrievableKafkaTaskInput.get() ++ retrievableTStreamTaskInput.get()
  }

  override def setConsumerOffset(envelope: Envelope): Unit = {
    envelope match {
      case tstreamEnvelope: TStreamEnvelope[T] =>
        retrievableTStreamTaskInput.setConsumerOffset(tstreamEnvelope)
      case kafkaEnvelope: KafkaEnvelope[T] =>
        retrievableKafkaTaskInput.setConsumerOffset(kafkaEnvelope)
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for batch streaming engine")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for batch streaming engine")
    }
  }

  override def setConsumerOffsetToLastEnvelope(): Unit = {
    retrievableKafkaTaskInput.setConsumerOffsetToLastEnvelope()
  }

  override def close(): Unit = {
    retrievableKafkaTaskInput.close()
    retrievableTStreamTaskInput.close()
  }
}
