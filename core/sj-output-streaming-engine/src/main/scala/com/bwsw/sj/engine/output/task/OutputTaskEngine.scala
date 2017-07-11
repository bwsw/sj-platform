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
package com.bwsw.sj.engine.output.task

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.common.engine.core.entities._
import com.bwsw.sj.common.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.common.engine.core.output.{Entity, OutputStreamingExecutor}
import com.bwsw.sj.common.si.model.instance.OutputInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.NumericalCheckpointTaskEngine
import com.bwsw.sj.engine.core.engine.input.CallableTStreamCheckpointTaskInput
import com.bwsw.sj.engine.output.processing.OutputProcessor
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.{Logger, LoggerFactory}
import scaldi.Injectable.inject
import scaldi.Injector


/**
  * Class contains methods for running output module
  *
  * @param manager            allows to manage an environment of output streaming task
  * @param performanceMetrics set of metrics that characterize performance of an output streaming module
  * @author Kseniya Mikhaleva
  */
abstract class OutputTaskEngine(manager: OutputTaskManager,
                                performanceMetrics: OutputStreamingPerformanceMetrics)
                               (implicit injector: Injector) extends TaskEngine {

  import OutputTaskEngine.logger

  private val currentThread: Thread = Thread.currentThread()
  currentThread.setName(s"output-task-${manager.taskName}-engine")
  private val blockingQueue: ArrayBlockingQueue[Envelope] = new ArrayBlockingQueue[Envelope](EngineLiterals.queueSize)
  private val instance: OutputInstance = manager.instance.asInstanceOf[OutputInstance]
  private val connectionRepository = inject[ConnectionRepository]
  private val streamService = connectionRepository.getStreamRepository
  private val outputStream: StreamDomain = getOutputStream
  private val environmentManager: OutputEnvironmentManager = createModuleEnvironmentManager()
  private val executor: OutputStreamingExecutor[AnyRef] = manager.getExecutor(environmentManager)
  private val entity: Entity[_] = executor.getOutputEntity
  val taskInputService: CallableTStreamCheckpointTaskInput[AnyRef] = new CallableTStreamCheckpointTaskInput[AnyRef](
    manager,
    blockingQueue,
    manager.createCheckpointGroup(),
    executor)
  private val outputProcessor: OutputProcessor[AnyRef] = OutputProcessor[AnyRef](
    outputStream,
    performanceMetrics,
    manager,
    entity)
  private var wasFirstCheckpoint: Boolean = false
  protected val checkpointInterval: Long = instance.checkpointInterval


  private def getOutputStream: StreamDomain =
    instance.outputs.flatMap(x => streamService.get(x)).head

  private def createModuleEnvironmentManager(): OutputEnvironmentManager = {
    val outputs = instance.outputs
      .flatMap(x => streamService.get(x))
    val options = instance.options
    new OutputEnvironmentManager(options, outputs, connectionRepository.getFileStorage)
  }

  /**
    * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
    *
    * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside output module
    *                              (not on the schedule) or not.
    */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean

  /**
    * It is in charge of running a basic execution logic of output task engine
    */
  override def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run output task engine in a separate thread of execution service.")

    while (true) {
      val maybeEnvelope = blockingQueue.poll(EngineLiterals.eventWaitTimeout, TimeUnit.MILLISECONDS)

      Option(maybeEnvelope) match {
        case Some(envelope) =>
          processOutputEnvelope(envelope)
        case _ =>
      }
      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }

  /**
    * Handler for sending data to storage.
    */
  private def processOutputEnvelope(envelope: Envelope): Unit = {
    afterReceivingEnvelope()
    val inputEnvelope = envelope.asInstanceOf[TStreamEnvelope[AnyRef]]
    registerInputEnvelope(inputEnvelope)
    logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler.")
    val outputEnvelopes = executor.onMessage(inputEnvelope)
    outputProcessor.process(outputEnvelopes, inputEnvelope, wasFirstCheckpoint)
  }

  /**
    * Register received envelope in performance metrics.
    *
    * @param envelope : received data
    */
  private def registerInputEnvelope(envelope: TStreamEnvelope[AnyRef]): Unit = {
    taskInputService.registerEnvelope(envelope)
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

  /**
    * Doing smth after process envelope.
    */
  protected def afterReceivingEnvelope(): Unit

  /**
    * Does group checkpoint of t-streams consumers/producers
    */
  protected def doCheckpoint(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint.")
    taskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint.")
    prepareForNextCheckpoint()
    wasFirstCheckpoint = true
  }

  protected def prepareForNextCheckpoint(): Unit

  def close(): Unit = {
    outputProcessor.close()
  }
}

object OutputTaskEngine {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Creates OutputTaskEngine is in charge of a basic execution logic of task of output module
    */
  def apply(manager: OutputTaskManager,
            performanceMetrics: OutputStreamingPerformanceMetrics)
           (implicit injector: Injector): OutputTaskEngine = {
    manager.outputInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.error(s"Task: ${manager.taskName}. Output module can't have a '${EngineLiterals.timeIntervalMode}'" +
          s" checkpoint mode.")
        throw new Exception(s"Task: ${manager.taskName}. Output module can't have a " +
          s"'${EngineLiterals.timeIntervalMode}' checkpoint mode.")

      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}. Output module has an '${EngineLiterals.everyNthMode}'" +
          s" checkpoint mode, create an appropriate task engine.")
        new OutputTaskEngine(manager, performanceMetrics) with NumericalCheckpointTaskEngine
    }
  }
}