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

/**
  * Simulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] for testing of
  * [[OutputStreamingExecutor]].
  *
  * Usage example:
  * {{{
  * val executor: OutputStreamingExecutor[(Integer, String)]
  *
  * val simulator = new OutputEngineSimulator[(Integer, String)](executor)
  * simulator.send(Seq((11, "a"), (24, "b")))
  * simulator.send(Seq((63, "c"), (31, "d")))
  * val requestBuilder = new RestRequestBuilder
  * val requests: Seq[String] = simulator.process(requestBuilder)
  * println(requests)
  * }}}
  *
  * @param executor testable [[OutputStreamingExecutor]]
  * @tparam DT type of incoming data
  * @author Pavel Tomskikh
  */
class OutputEngineSimulator[DT <: AnyRef](executor: OutputStreamingExecutor[DT]) {

  import OutputEngineSimulator.defaultConsumerName

  private val inputEnvelopes: mutable.Buffer[TStreamEnvelope[DT]] = mutable.Buffer.empty
  private var envelopeId: Long = 0

  /**
    * Creates [[TStreamEnvelope]] and saves it in a local buffer
    *
    * @param entities     incoming data
    * @param consumerName name of consumer
    */
  def send(entities: Seq[DT], consumerName: String = defaultConsumerName): Unit = {
    val queue = mutable.Queue(entities: _*)
    val envelope = new TStreamEnvelope[DT](queue, consumerName)
    envelope.id = envelopeId
    envelopeId += 1

    inputEnvelopes += envelope
  }

  /**
    * Sends all [[TStreamEnvelope]]s from local buffer to [[OutputStreamingExecutor]] and builds requests for output
    * service
    *
    * @param outputRequestBuilder builder of requests for output service
    * @param clearBuffer          indicates that local buffer must be cleared
    * @tparam ET type of data elements in [[Entity]]
    * @return collection of requests
    */
  def process[ET](outputRequestBuilder: OutputRequestBuilder[ET], clearBuffer: Boolean = true): Seq[String] = {
    val outputEntity: Entity[ET] = executor.getOutputEntity.asInstanceOf[Entity[ET]]
    val requests = inputEnvelopes.flatMap { envelope =>
      val outputEnvelopes = executor.onMessage(envelope)
      outputEnvelopes.map(outputRequestBuilder.build(_, envelope, outputEntity))
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
