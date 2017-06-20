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
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor

import scala.collection.mutable

/**
  * Simulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] for testing of
  * [[OutputStreamingExecutor]].
  *
  * Usage example:
  * {{{
  * val manager: OutputEnvironmentManager
  * val executor: OutputStreamingExecutor[(Integer, String)] = new MyOutputExecutor(manager)
  *
  * val requestBuilder = new RestRequestBuilder
  * val simulator = new OutputEngineSimulator(executor, requestBuilder, manager)
  * simulator.prepare(Seq((1, "a"), (2, "b")))
  * simulator.prepare(Seq((3, "c")))
  * val requestsBeforeFirstCheckpoint = simulator.process()
  * println(requestsBeforeFirstCheckpoint)
  *
  * simulator.wasFirstCheckpoint = true // first checkpoint was performed
  * simulator.prepare(Seq((4, "d"), (5, "e")))
  * val requestsAfterFirstCheckpoint = simulator.process()
  * println(requestsAfterFirstCheckpoint)
  * }}}
  * The [[OutputEngineSimulator]] automatically updates a wasFirstCheckpoint if [[executor]] invokes a
  * [[manager.initiateCheckpoint()]].
  *
  * @param executor             class under test [[OutputStreamingExecutor]]
  * @param outputRequestBuilder builder of requests for output service
  * @param manager              environment manager that used by executor
  * @tparam IT type of incoming data
  * @tparam OT type of requests for output service
  * @author Pavel Tomskikh
  */
class OutputEngineSimulator[IT <: AnyRef, OT](executor: OutputStreamingExecutor[IT],
                                              outputRequestBuilder: OutputRequestBuilder[OT],
                                              manager: OutputEnvironmentManager) {

  import OutputEngineSimulator.defaultConsumerName

  /**
    * Indicates that first checkpoint was performed
    */
  var wasFirstCheckpoint: Boolean = false
  private val inputEnvelopes: mutable.Buffer[TStreamEnvelope[IT]] = mutable.Buffer.empty
  private var transactionId: Long = 0

  /**
    * Creates [[TStreamEnvelope]] and saves it in a local buffer
    *
    * @param entities     incoming data
    * @param consumerName name of consumer
    * @return ID of saved transaction
    */
  def prepare(entities: Seq[IT], consumerName: String = defaultConsumerName): Long = {
    val queue = mutable.Queue(entities: _*)
    val envelope = new TStreamEnvelope[IT](queue, consumerName)
    transactionId += 1
    envelope.id = transactionId

    inputEnvelopes += envelope
    transactionId
  }

  /**
    * Sends all [[TStreamEnvelope]]s from local buffer to [[OutputStreamingExecutor]] and builds requests for output
    * service
    *
    * @param clearBuffer indicates that local buffer must be cleared
    * @return collection of requests
    */
  def process(clearBuffer: Boolean = true): Seq[OT] = {
    val requests = inputEnvelopes.flatMap { inputEnvelope =>
      val outputEnvelopes = executor.onMessage(inputEnvelope)
      val deletionRequest = {
        if (wasFirstCheckpoint) Seq.empty
        else {
          if (manager.isCheckpointInitiated) {
            wasFirstCheckpoint = true
            manager.isCheckpointInitiated = false
          }
          Seq(outputRequestBuilder.buildDelete(inputEnvelope))
        }
      }

      deletionRequest ++ outputEnvelopes.map(outputRequestBuilder.buildInsert(_, inputEnvelope))
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
