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

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.common.si.model.instance.{BatchInstance, Instance, RegularInstance}
import com.bwsw.sj.common.utils.{EngineLiterals, SjTimer}
import com.bwsw.sj.common.engine.core.environment.{ModuleEnvironmentManager, ModuleOutput}
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.Producer
import org.slf4j.{Logger, LoggerFactory}
import scaldi.Injector

import scala.collection.mutable

/**
  * Class is in charge of creating a specific ModuleEnvironmentManager (and executor)
  * depending on an instance parameter [[com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain.stateManagement]] and performing the appropriate actions related with checkpoint
  *
  * @param manager            manager of environment of task of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  * @param performanceMetrics set of metrics that characterize performance of a regular or batch streaming module
  */
abstract class CommonModuleService(manager: CommonTaskManager,
                                   checkpointGroup: CheckpointGroup,
                                   performanceMetrics: PerformanceMetrics) {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val instance: Instance = manager.instance
  protected val outputProducers: Map[String, Producer] = manager.outputProducers
  val moduleTimer: SjTimer = new SjTimer()
  protected val producerPolicyByOutput: mutable.Map[String, (String, ModuleOutput)] = mutable.Map[String, (String, ModuleOutput)]()

  addProducersToCheckpointGroup()

  protected val environmentManager: ModuleEnvironmentManager
  val executor: StreamingExecutor

  private def addProducersToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group.")
    outputProducers.foreach(x => {
      checkpointGroup.add(x._2)
    })
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group.")
  }

  def doCheckpoint(): Unit = {
    producerPolicyByOutput.foreach(_._2._2.clear())
  }

  def isCheckpointInitiated: Boolean = environmentManager.isCheckpointInitiated
}

object CommonModuleService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(manager: CommonTaskManager,
            checkpointGroup: CheckpointGroup,
            performanceMetrics: PerformanceMetrics)
           (implicit injector: Injector): CommonModuleService = {

    val stateManagement = manager.instance match {
      case regularInstance: RegularInstance =>
        regularInstance.stateManagement
      case batchInstance: BatchInstance =>
        batchInstance.stateManagement
      case _ =>
        logger.error(s"CommonModuleService can be used only for ${EngineLiterals.regularStreamingType} or ${EngineLiterals.batchStreamingType} engine.")
        throw new RuntimeException(s"CommonModuleService can be used only for ${EngineLiterals.regularStreamingType} or ${EngineLiterals.batchStreamingType} engine.")
    }

    stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of batch module without a state.")
        new StatelessCommonModuleService(manager, checkpointGroup, performanceMetrics)
      case EngineLiterals.ramStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of batch module with a state.")
        new StatefulCommonModuleService(manager, checkpointGroup, performanceMetrics)
    }
  }
}