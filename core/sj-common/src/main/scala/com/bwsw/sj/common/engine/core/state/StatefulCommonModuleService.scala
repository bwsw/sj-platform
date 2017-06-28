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

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.engine.{StateHandlers, StreamingExecutor}
import com.bwsw.sj.common.si.model.instance.{BatchInstance, RegularInstance}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.engine.core.environment.StatefulModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Class is in charge of creating [[StatefulModuleEnvironmentManager]] (and executor [[StreamingExecutor]] with [[StateHandlers]])
  * and saving of state
  *
  * @param manager            manager of environment of task of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  * @param checkpointGroup    group of t-stream agents that have to make a checkpoint at the same time
  * @param performanceMetrics set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  */
class StatefulCommonModuleService(manager: CommonTaskManager,
                                  checkpointGroup: CheckpointGroup,
                                  performanceMetrics: PerformanceMetrics)
                                 (implicit injector: Injector)
  extends CommonModuleService(manager, checkpointGroup, performanceMetrics) {

  private val streamService: GenericMongoRepository[StreamDomain] = inject[ConnectionRepository].getStreamRepository
  private var countOfCheckpoints: Int = 1
  private val stateService: RAMStateService = new RAMStateService(manager, checkpointGroup)

  val environmentManager = new StatefulModuleEnvironmentManager(
    new StateStorage(stateService),
    instance.options,
    outputProducers,
    instance.outputs.flatMap(x => streamService.get(x)),
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics,
    manager.moduleClassLoader
  )

  val executor: StreamingExecutor = manager.getExecutor(environmentManager)

  private val statefulExecutor: StateHandlers = executor.asInstanceOf[StateHandlers]

  /**
    * Does group checkpoint of t-streams state consumers/producers
    */
  override def doCheckpoint(): Unit = {
    if (countOfCheckpoints != getStateFullCheckpoint()) {
      doCheckpointOfPartOfState()
    } else {
      doCheckpointOfFullState()
    }
    super.doCheckpoint()
  }

  /**
    * Saves a partial state changes
    */
  private def doCheckpointOfPartOfState(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of a part of state.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler.")
    statefulExecutor.onBeforeStateSave(false)
    stateService.savePartialState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler.")
    statefulExecutor.onAfterStateSave(false)
    countOfCheckpoints += 1
  }

  /**
    * Saves a state
    */
  private def doCheckpointOfFullState(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of full state.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler.")
    statefulExecutor.onBeforeStateSave(true)
    stateService.saveFullState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler.")
    statefulExecutor.onAfterStateSave(true)
    countOfCheckpoints = 1
  }

  private def getStateFullCheckpoint(): Int = {
    instance match {
      case regularInstance: RegularInstance => regularInstance.stateFullCheckpoint
      case batchInstance: BatchInstance => batchInstance.stateFullCheckpoint
    }
  }
}