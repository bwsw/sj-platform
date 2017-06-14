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
package com.bwsw.sj.engine.core.simulation

import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.engine.core.output.{Entity, OutputStreamingExecutor}

import scala.collection.mutable

//todo: add usage example
/**
  * Simulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] for testing of
  * [[OutputStreamingExecutor]].
  *
  * @param executor testable [[OutputStreamingExecutor]]
  * @tparam T type of incoming data
  * @author Pavel Tomskikh
  */
class OutputEngineSimulator[T <: AnyRef](executor: OutputStreamingExecutor[T]) {

  import OutputEngineSimulator.defaultConsumerName

  private val inputEnvelopes: mutable.Buffer[TStreamEnvelope[T]] = mutable.Buffer.empty
  private val outputEntity: Entity[_] = executor.getOutputEntity

  /**
    * Creates [[TStreamEnvelope]] and saves it in a local buffer
    *
    * @param entities     incoming data
    * @param consumerName name of consumer
    */
  def send(entities: Seq[T], consumerName: String = defaultConsumerName): Unit = {
    val queue = mutable.Queue(entities: _*)

    inputEnvelopes += new TStreamEnvelope[T](queue, consumerName)
  }

  /**
    * Sends all [[TStreamEnvelope]]s from local buffer to [[OutputStreamingExecutor]] and builds requests for output
    * service
    *
    * @param outputRequestBuilder builder of requests for output service
    * @param clearBuffer          indicates that local buffer must be cleared
    * @return
    */
  def process(outputRequestBuilder: OutputRequestBuilder, clearBuffer: Boolean = true): Seq[String] = {
    val requests = inputEnvelopes.flatMap { envelope =>
      val outputEnvelopes = executor.onMessage(envelope)
      outputEnvelopes.map(outputRequestBuilder.build(outputEntity))
    }

    if (clearBuffer) clear()

    requests
  }

  /**
    * Removes all envelopes from local buffer
    */
  def clear(): Unit = inputEnvelopes.clear()
}

object OutputEngineSimulator {
  val defaultConsumerName = "default-consumer-name"
}
