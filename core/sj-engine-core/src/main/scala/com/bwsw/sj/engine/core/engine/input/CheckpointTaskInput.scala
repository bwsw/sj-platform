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

import java.io.Closeable

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.tstreams.agents.group.CheckpointGroup

import scala.collection.mutable

/**
  * Class is able to do checkpoint of processing messages
  *
  * @param inputs set of streams and their set of partitions that this task input is responsible for
  *
  * @author Kseniya Mikhaleva
  */
abstract class CheckpointTaskInput[E <: Envelope](inputs: scala.collection.mutable.Map[StreamDomain, Array[Int]]) extends Closeable {
  private val lastEnvelopesByStreams: mutable.Map[(String, Int), Envelope] = createStorageOfLastEnvelopes()
  val checkpointGroup: CheckpointGroup

  private def createStorageOfLastEnvelopes(): mutable.Map[(String, Int), Envelope] = {
    inputs.flatMap(x => x._2.map(y => ((x._1.name, y), new Envelope())))
  }

  def registerEnvelope(envelope: E): Unit = {
    lastEnvelopesByStreams((envelope.stream, envelope.partition)) = envelope
  }

  def setConsumerOffsetToLastEnvelope(): Unit = {
    lastEnvelopesByStreams.values.filterNot(_.isEmpty()).foreach(envelope => {
      setConsumerOffset(envelope.asInstanceOf[E])
    })
    lastEnvelopesByStreams.clear()
  }

  protected def setConsumerOffset(envelope: E): Unit

  def doCheckpoint(): Unit = {
    setConsumerOffsetToLastEnvelope()
    checkpointGroup.checkpoint()
  }
}