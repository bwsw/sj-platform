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

import java.util.concurrent.{ArrayBlockingQueue, Callable}

import com.bwsw.common.SerializerInterface
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.common.engine.core.entities.Envelope
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory
import scaldi.Injector

abstract class CallableCheckpointTaskInput[T <: Envelope](inputs: scala.collection.mutable.Map[StreamDomain, Array[Int]])
  extends CheckpointTaskInput[T](inputs) with Callable[Unit]

object CallableCheckpointTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply[T <: AnyRef](manager: CommonTaskManager,
                         blockingQueue: ArrayBlockingQueue[Envelope],
                         checkpointGroup: CheckpointGroup,
                         envelopeDataSerializer: SerializerInterface)
                        (implicit injector: Injector): CallableCheckpointTaskInput[_ <: Envelope] = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new CallableCompleteCheckpointTaskInput[T](
        manager,
        blockingQueue,
        checkpointGroup,
        envelopeDataSerializer)

      case (false, true) => new CallableTStreamCheckpointTaskInput[T](
        manager,
        blockingQueue,
        checkpointGroup,
        envelopeDataSerializer)

      case (true, false) => new CallableKafkaCheckpointTaskInput[T](
        manager,
        blockingQueue,
        checkpointGroup,
        envelopeDataSerializer)

      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new Exception("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}