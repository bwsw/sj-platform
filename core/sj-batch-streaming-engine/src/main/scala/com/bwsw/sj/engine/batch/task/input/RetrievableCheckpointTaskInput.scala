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
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.engine.core.entities.Envelope
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.input.CheckpointTaskInput
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory
import scaldi.Injector

import scala.collection.mutable

/**
  * Class is responsible for handling an input streams of specific type,
  * i.e. for consuming and processing the incoming envelopes
  *
  * @author Kseniya Mikhaleva
  */
abstract class RetrievableCheckpointTaskInput[T <: Envelope](val inputs: mutable.Map[StreamDomain, Array[Int]])
  extends CheckpointTaskInput[T](inputs) {
  def get(): Iterable[T]
}

object RetrievableCheckpointTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply[T <: AnyRef](manager: CommonTaskManager,
                         checkpointGroup: CheckpointGroup,
                         envelopeDataSerializer: SerializerInterface)
                        (implicit injector: Injector): RetrievableCheckpointTaskInput[_ <: Envelope] = {
    val kafkaInputExists = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val tstreamInputExists = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (kafkaInputExists, tstreamInputExists) match {
      case (true, true) => new RetrievableCompleteCheckpointTaskInput[T](manager, checkpointGroup, envelopeDataSerializer)
      case (false, true) => new RetrievableTStreamCheckpointTaskInput[T](manager, checkpointGroup, envelopeDataSerializer)
      case (true, false) => new RetrievableKafkaCheckpointTaskInput[T](manager, checkpointGroup, envelopeDataSerializer)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new RuntimeException("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}