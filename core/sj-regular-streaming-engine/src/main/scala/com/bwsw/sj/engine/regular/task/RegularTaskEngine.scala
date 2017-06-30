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
package com.bwsw.sj.engine.regular.task

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.common.si.model.instance.RegularInstance
import com.bwsw.sj.common.utils.{EngineLiterals, SjTimer}
import com.bwsw.sj.engine.core.engine.input.CallableCheckpointTaskInput
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.common.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.common.engine.core.state.{CommonModuleService, StatefulCommonModuleService, StatelessCommonModuleService}
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.{Logger, LoggerFactory}
import scaldi.Injector

/**
  * Class contains methods for running regular module
  *
  * @param manager            allows to manage an environment of regular streaming task
  * @param performanceMetrics set of metrics that characterize performance of a regular streaming module
  * @author Kseniya Mikhaleva
  */
abstract class RegularTaskEngine(manager: CommonTaskManager,
                                 performanceMetrics: RegularStreamingPerformanceMetrics)
                                (implicit injector: Injector) extends TaskEngine {

  import RegularTaskEngine.logger

  private val currentThread: Thread = Thread.currentThread()
  currentThread.setName(s"regular-task-${manager.taskName}-engine")
  private val blockingQueue: ArrayBlockingQueue[Envelope] = new ArrayBlockingQueue[Envelope](EngineLiterals.queueSize)
  private val instance: RegularInstance = manager.instance.asInstanceOf[RegularInstance]
  private val checkpointGroup: CheckpointGroup = manager.createCheckpointGroup()
  private val moduleService: CommonModuleService = createRegularModuleService()
  private val executor: RegularStreamingExecutor[AnyRef] = moduleService.executor.asInstanceOf[RegularStreamingExecutor[AnyRef]]
  val taskInputService: CallableCheckpointTaskInput[Envelope] = CallableCheckpointTaskInput[AnyRef](
    manager,
    blockingQueue,
    checkpointGroup,
    executor)
    .asInstanceOf[CallableCheckpointTaskInput[Envelope]]
  private val moduleTimer: SjTimer = moduleService.moduleTimer
  protected val checkpointInterval: Long = instance.checkpointInterval

  private def createRegularModuleService(): CommonModuleService = {
    instance.stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of regular module without state.")
        new StatelessCommonModuleService(manager, checkpointGroup, performanceMetrics)
      case EngineLiterals.ramStateMode =>
        new StatefulCommonModuleService(manager, checkpointGroup, performanceMetrics)
    }
  }

  /**
    * It is in charge of running a basic execution logic of regular task engine
    */
  override def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run regular task engine in a separate thread of execution service.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onInit() handler.")
    executor.onInit()

    while (true) {
      val maybeEnvelope = blockingQueue.poll(instance.eventWaitIdleTime, TimeUnit.MILLISECONDS)

      Option(maybeEnvelope) match {
        case Some(envelope) =>
          registerEnvelope(envelope)
          logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler.")
          envelope match {
            case kafkaEnvelope: KafkaEnvelope[AnyRef@unchecked] =>
              executor.onMessage(kafkaEnvelope)
            case tstreamEnvelope: TStreamEnvelope[AnyRef@unchecked] =>
              executor.onMessage(tstreamEnvelope)
            case wrongEnvelope =>
              logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/batch" +
                s" streaming engine")
              throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for " +
                s"regular/batch streaming engine")
          }

        case None =>
          performanceMetrics.increaseTotalIdleTime(instance.eventWaitIdleTime)
          executor.onIdle()
      }

      if (isItTimeToCheckpoint(moduleService.isCheckpointInitiated)) doCheckpoint()

      if (moduleTimer.isTime) {
        logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler.")
        executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
        moduleTimer.reset()
      }
    }
  }

  private def registerEnvelope(envelope: Envelope): Unit = {
    afterReceivingEnvelope()
    taskInputService.registerEnvelope(envelope)
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

  protected def afterReceivingEnvelope(): Unit

  /**
    * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
    *
    * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside regular module
    *                              (not on the schedule) or not.
    */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean

  /**
    * Does group checkpoint of t-streams consumers/producers
    */
  private def doCheckpoint(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler.")
    executor.onBeforeCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint.")
    moduleService.doCheckpoint()
    taskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler.")
    executor.onAfterCheckpoint()
    prepareForNextCheckpoint()
  }

  protected def prepareForNextCheckpoint(): Unit
}

object RegularTaskEngine {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Creates RegularTaskEngine is in charge of a basic execution logic of task of regular module
    *
    * @return Engine of regular task
    */
  def apply(manager: CommonTaskManager,
            performanceMetrics: RegularStreamingPerformanceMetrics)
           (implicit injector: Injector): RegularTaskEngine = {
    val regularInstance = manager.instance.asInstanceOf[RegularInstance]

    regularInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.info(s"Task: ${manager.taskName}. Regular module has a '${EngineLiterals.timeIntervalMode}' " +
          s"checkpoint mode, create an appropriate task engine.")
        new RegularTaskEngine(manager, performanceMetrics) with TimeCheckpointTaskEngine
      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}. Regular module has an '${EngineLiterals.everyNthMode}' " +
          s"checkpoint mode, create an appropriate task engine.")
        new RegularTaskEngine(manager, performanceMetrics) with NumericalCheckpointTaskEngine

    }
  }
}