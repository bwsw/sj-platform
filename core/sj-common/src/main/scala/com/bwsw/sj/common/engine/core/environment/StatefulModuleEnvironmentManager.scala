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
package com.bwsw.sj.common.engine.core.environment

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.engine.core.state.StateStorage
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.tstreams.agents.producer.Producer

import scala.collection.{mutable, _}

/**
  * Class allowing to manage environment of module that has got a state
  *
  * @param stateStorage           storage of state of module [[com.bwsw.sj.common.engine.core.state.StateStorage]]
  * @param producers              t-streams producers for each output stream from instance
  *                               [[com.bwsw.sj.common.dal.model.instance.InstanceDomain.outputs]]
  * @param options                user defined options from instance
  *                               [[com.bwsw.sj.common.dal.model.instance.InstanceDomain.options]]
  * @param outputs                set of output streams [[com.bwsw.sj.common.dal.model.stream.StreamDomain]]
  *                               from instance [[com.bwsw.sj.common.dal.model.instance.InstanceDomain.outputs]]
  * @param producerPolicyByOutput keeps a tag (partitioned or round-robin output) corresponding to the output for each
  *                               output stream
  * @param moduleTimer            provides a possibility to set a timer inside a module
  * @param performanceMetrics     set of metrics that characterize performance
  *                               of [[com.bwsw.sj.common.utils.EngineLiterals.regularStreamingType]]
  *                               or [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] module
  * @param fileStorage            file storage
  * @author Kseniya Mikhaleva
  */

class StatefulModuleEnvironmentManager(stateStorage: StateStorage,
                                       options: String,
                                       producers: Map[String, Producer],
                                       outputs: Array[StreamDomain],
                                       producerPolicyByOutput: mutable.Map[String, (String, ModuleOutput)],
                                       moduleTimer: SjTimer,
                                       performanceMetrics: PerformanceMetrics,
                                       fileStorage: FileStorage)
  extends ModuleEnvironmentManager(
    options,
    producers,
    outputs,
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics,
    fileStorage) {

  override def getState: StateStorage = {
    logger.info(s"Get a storage where states are kept.")
    stateStorage
  }
}
