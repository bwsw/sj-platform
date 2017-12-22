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
package com.bwsw.sj.engine.core.simulation.output

import com.bwsw.sj.common.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.common.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.common.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.simulation.SimulatorConstants.{defaultConsumerName, defaultInputStream}

import scala.collection.mutable

/**
  * Simulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] for testing of
  * [[com.bwsw.sj.common.engine.core.output.OutputStreamingExecutor]].
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
  * simulator.beforeFirstCheckpoint = false // first checkpoint was performed
  * simulator.prepare(Seq((4, "d"), (5, "e")))
  * val requestsAfterFirstCheckpoint = simulator.process()
  * println(requestsAfterFirstCheckpoint)
  * }}}
  * The [[OutputEngineSimulator]] automatically updates a wasFirstCheckpoint if executor invokes a
  * [[com.bwsw.sj.common.engine.core.environment.OutputEnvironmentManager.initiateCheckpoint]]().
  *
  * @param executor             implementation of [[com.bwsw.sj.common.engine.core.output.OutputStreamingExecutor]] under tests
  * @param outputRequestBuilder builder of requests for output service
  * @param manager              environment manager that used by executor
  * @tparam IT type of incoming data
  * @tparam OT type of requests for output service
  * @author Pavel Tomskikh
  */
class OutputEngineSimulator[IT <: AnyRef, OT](executor: OutputStreamingExecutor[IT],
                                              outputRequestBuilder: OutputRequestBuilder[OT],
                                              manager: OutputEnvironmentManager) {

  /**
    * Indicates that first checkpoint was not performed
    */
  var beforeFirstCheckpoint: Boolean = true
  private val inputEnvelopes: mutable.Buffer[TStreamEnvelope[IT]] = mutable.Buffer.empty
  private var transactionId: Long = 0

  /**
    * Creates [[com.bwsw.sj.common.engine.core.entities.TStreamEnvelope]] and saves it in a local buffer
    *
    * @param entities     incoming data
    * @param stream       name of stream in which will be that envelope
    * @param consumerName name of consumer
    * @return ID of saved transaction
    */
  def prepare(entities: Seq[IT],
              stream: String = defaultInputStream,
              consumerName: String = defaultConsumerName): Long = {
    val queue = mutable.Queue(entities: _*)
    val envelope = new TStreamEnvelope[IT](queue, consumerName)
    transactionId += 1
    envelope.id = transactionId
    envelope.stream = stream

    inputEnvelopes += envelope
    transactionId
  }

  /**
    * Sends all [[com.bwsw.sj.common.engine.core.entities.TStreamEnvelope]]s from local buffer
    * to [[com.bwsw.sj.common.engine.core.output.OutputStreamingExecutor]] and builds requests for output service
    *
    * @param clearBuffer indicates that local buffer must be cleared
    * @return collection of requests
    */
  def process(clearBuffer: Boolean = true): Seq[OT] = {
    val requests = inputEnvelopes.flatMap { inputEnvelope =>
      val outputEnvelopes = executor.onMessage(inputEnvelope)
      val deletionRequest = {
        if (beforeFirstCheckpoint) {
          if (manager.isCheckpointInitiated) {
            beforeFirstCheckpoint = false
            manager.isCheckpointInitiated = false
          }
          Seq(outputRequestBuilder.buildDelete(inputEnvelope))
        }
        else Seq.empty
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
