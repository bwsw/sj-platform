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
package com.bwsw.sj.engine.core.simulation.regular

import com.bwsw.sj.common.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.simulation.SimulatorConstants.defaultConsumerName
import com.bwsw.sj.engine.core.simulation.state._

import scala.collection.mutable

/**
  * Simulates behavior of [[com.bwsw.sj.common.engine.TaskEngine TaskEngine]] for testing an implementation of
  * [[RegularStreamingExecutor]]
  *
  * Usage example:
  * {{{
  * val stateSaver = mock(classOf[StateSaverInterface])
  * val stateLoader = new StateLoaderMock
  * val stateService = new RAMStateService(stateSaver, stateLoader)
  * val stateStorage = new StateStorage(stateService)
  * val options = ""
  * val output = new TStreamStreamDomain("out", mock(classOf[TStreamServiceDomain]), 3, tags = Array("output"))
  * val manager = new ModuleEnvironmentManagerMock(stateStorage, options, Array(output))
  * val executor: RegularStreamingExecutor[String] = new MyExecutor(manager)
  * val tstreamInput = "t-stream-input"
  * val kafkaInput = "kafka-input"
  *
  * val simulator = new RegularEngineSimulator(executor, manager)
  * simulator.prepareState(Map("idleCalls" -> 0, "symbols" -> 0))
  * simulator.prepareTstream(Seq("ab", "c", "de"), tstreamInput)
  * simulator.prepareKafka(Seq("fgh", "g"), kafkaInput)
  * simulator.prepareTstream(Seq("ijk", "lm"), tstreamInput)
  *
  * val envelopesNumberBeforeIdle = 2
  * val results = simulator.process(envelopesNumberBeforeIdle)
  * println(results)
  * }}}
  *
  * @param executor implementation of [[RegularStreamingExecutor]] under test
  * @param manager  environment manager that used by executor
  * @tparam T type of incoming data
  * @author Pavel Tomskikh
  */
class RegularEngineSimulator[T <: AnyRef](executor: RegularStreamingExecutor[T],
                                          manager: ModuleEnvironmentManagerMock)
  extends CommonEngineSimulator[T](executor, manager) {

  /**
    * Sends all incoming envelopes from local buffer to [[executor]] and returns output elements and state
    *
    * @param envelopesNumberBeforeIdle number of envelopes between invocations of [[executor.onIdle()]].
    *                                  '0' means that [[executor.onIdle()]] will never be called.
    * @param clearBuffer               indicates that local buffer must be cleared
    * @return output elements and state
    */
  def process(envelopesNumberBeforeIdle: Int = 0, clearBuffer: Boolean = true): SimulationResult = {
    var envelopesAfterIdle: Int = 0

    inputEnvelopes.foreach { envelope =>
      envelope match {
        case tStreamEnvelope: TStreamEnvelope[T] =>
          executor.onMessage(tStreamEnvelope)
        case kafkaEnvelope: KafkaEnvelope[T] =>
          executor.onMessage(kafkaEnvelope)
      }


      if (envelopesNumberBeforeIdle > 0) {
        envelopesAfterIdle += 1
        if (envelopesAfterIdle == envelopesNumberBeforeIdle) {
          executor.onIdle()
          envelopesAfterIdle = 0
        }
      }
    }

    if (clearBuffer) clear()

    simulationResult
  }

  /**
    * Removes all envelopes from local buffer
    */
  def clear(): Unit =
    inputEnvelopes.clear()
}
