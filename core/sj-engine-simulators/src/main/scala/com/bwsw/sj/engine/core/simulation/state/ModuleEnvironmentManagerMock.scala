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
package com.bwsw.sj.engine.core.simulation.state

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.engine.core.environment.{ModuleOutput, StatefulModuleEnvironmentManager}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.engine.core.state.StateStorage
import com.bwsw.sj.common.utils.SjTimer
import org.mockito.Mockito.mock

import scala.collection.mutable

/**
  * Mock for [[com.bwsw.sj.common.engine.core.environment.StatefulModuleEnvironmentManager]].
  * Creates [[PartitionedOutputMock]] instead [[com.bwsw.sj.common.engine.core.environment.PartitionedOutput]]
  * and [[RoundRobinOutputMock]] instead [[com.bwsw.sj.common.engine.core.environment.RoundRobinOutput]].
  *
  * @param stateStorage storage of state
  * @param options      user defined options from instance
  * @param outputs      set of output streams from instance
  * @param fileStorage  file storage
  * @param senderThread mock for thread for sending data to the T-Streams service
  * @author Pavel Tomskikh
  */
class ModuleEnvironmentManagerMock(stateStorage: StateStorage,
                                   options: String,
                                   outputs: Array[TStreamStreamDomain],
                                   val senderThread: TStreamsSenderThreadMock,
                                   fileStorage: FileStorage = mock(classOf[FileStorage]))
  extends {
    val producerPolicyByOutput = mutable.Map.empty[String, ModuleOutput]
    val moduleTimer = mock(classOf[SjTimer])
    val performanceMetrics = mock(classOf[PerformanceMetrics])
  } with StatefulModuleEnvironmentManager(
    stateStorage,
    options,
    outputs.asInstanceOf[Array[StreamDomain]],
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics,
    fileStorage,
    senderThread) {

  /**
    * Allows getting partitioned output for specific output stream
    *
    * @param streamName Name of output stream
    * @return Partitioned output that wrapping output stream
    */
  override def getPartitionedOutput(streamName: String)
                                   (implicit serialize: (AnyRef) => Array[Byte]): PartitionedOutputMock =
    getOutput(streamName, () => new PartitionedOutputMock(outputs.find(_.name == streamName).get, senderThread))

  /**
    * Allows getting round-robin output for specific output stream
    *
    * @param streamName Name of output stream
    * @return Round-robin output that wrapping output stream
    */
  override def getRoundRobinOutput(streamName: String)
                                  (implicit serialize: (AnyRef) => Array[Byte]): RoundRobinOutputMock =
    getOutput(streamName, () => new RoundRobinOutputMock(outputs.find(_.name == streamName).get, senderThread))
}
