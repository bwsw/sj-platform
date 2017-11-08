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

import com.bwsw.sj.common.engine.core.environment.{ModuleEnvironmentManager, ModuleOutput, TStreamsSenderThread}
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.engine.{StreamingExecutor, TimerHandlers}
import com.bwsw.sj.common.si.model.instance.{BatchInstance, Instance, RegularInstance}
import com.bwsw.sj.common.utils.{EngineLiterals, SjTimer}
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.Producer
import com.typesafe.scalalogging.Logger
import scaldi.Injector

import scala.collection.mutable

/**
  * Class is in charge of creating a specific ModuleEnvironmentManager (and executor)
  * depending on an instance parameter [[com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain.stateManagement]]
  * and performing the appropriate actions related with checkpoint
  */
abstract class CommonModuleService(protected val instance: Instance,
                                   protected val outputProducers: Map[String, Producer],
                                   checkpointGroup: CheckpointGroup) {

  protected val logger: Logger = Logger(this.getClass)
  protected val moduleTimer: SjTimer = new SjTimer()
  protected val producerPolicyByOutput: mutable.Map[String, ModuleOutput] = mutable.Map.empty
  addProducersToCheckpointGroup()
  protected val senderThread: TStreamsSenderThread

  protected val environmentManager: ModuleEnvironmentManager
  val executor: StreamingExecutor with TimerHandlers

  private def addProducersToCheckpointGroup(): Unit = {
    logger.debug(s"Start adding t-stream producers to checkpoint group.")
    outputProducers.foreach(x => {
      checkpointGroup.add(x._2)
    })
    logger.debug(s"The t-stream producers are added to checkpoint group.")
  }

  def prepareToCheckpoint(): Unit =
    senderThread.prepareToCheckpoint()

  def isCheckpointInitiated: Boolean = environmentManager.isCheckpointInitiated

  def onTimer(): Unit = {
    if (moduleTimer.isTime) {
      logger.debug(s"Invoke onTimer() handler.")
      executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
      moduleTimer.reset()
    }
  }
}

object CommonModuleService {
  private val logger = Logger(this.getClass)

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
        val message = s"CommonModuleService can be used only for ${EngineLiterals.regularStreamingType}" +
          s" or ${EngineLiterals.batchStreamingType} engine."
        logger.error(message)
        throw new RuntimeException(message)
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