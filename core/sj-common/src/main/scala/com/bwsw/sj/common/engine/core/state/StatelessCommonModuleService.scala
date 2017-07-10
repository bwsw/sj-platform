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
package com.bwsw.sj.common.engine.core.state

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.engine.{StreamingExecutor, TimerHandlers}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.tstreams.agents.group.CheckpointGroup
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Class is in charge of creating [[ModuleEnvironmentManager]] (and executor [[StreamingExecutor]])
  *
  * @param manager            manager of environment of task of [[EngineLiterals.regularStreamingType]]
  *                           or [[EngineLiterals.batchStreamingType]] module
  * @param performanceMetrics set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]]
  *                           or [[EngineLiterals.batchStreamingType]] module
  */
class StatelessCommonModuleService(manager: CommonTaskManager,
                                   checkpointGroup: CheckpointGroup,
                                   performanceMetrics: PerformanceMetrics)
                                  (implicit injector: Injector)
  extends CommonModuleService(manager.instance, manager.outputProducers, checkpointGroup) {

  private val connectionRepository = inject[ConnectionRepository]
  private val streamService = inject[ConnectionRepository].getStreamRepository

  val environmentManager: ModuleEnvironmentManager = new ModuleEnvironmentManager(
    instance.options,
    outputProducers,
    instance.outputs.flatMap(x => streamService.get(x)),
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics,
    connectionRepository)

  val executor: StreamingExecutor with TimerHandlers = manager.getExecutor(environmentManager).asInstanceOf[StreamingExecutor with TimerHandlers]
}
