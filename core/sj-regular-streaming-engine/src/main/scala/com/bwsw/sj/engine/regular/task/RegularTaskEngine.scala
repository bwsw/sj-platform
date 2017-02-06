package com.bwsw.sj.engine.regular.task

import java.util.concurrent.Callable

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.input.{CallableTaskInput, CallableTaskInput$}
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, PersistentBlockingQueue, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.{CommonModuleService, StatefulCommonModuleService, StatelessCommonModuleService}
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Provides methods are responsible for a basic execution logic of task of regular module
 *
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module

 * @author Kseniya Mikhaleva
 */
abstract class RegularTaskEngine(protected val manager: CommonTaskManager,
                                 performanceMetrics: RegularStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"regular-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(EngineLiterals.persistentBlockingQueue)
  private val instance = manager.instance.asInstanceOf[RegularInstance]
  val taskInputService = CallableTaskInput(manager, blockingQueue)
  private val moduleService = createRegularModuleService()
  private val executor = moduleService.executor.asInstanceOf[RegularStreamingExecutor]
  private val moduleTimer = moduleService.moduleTimer
  protected val checkpointInterval = instance.checkpointInterval

  private val envelopeSerializer = new JsonSerializer(true)

  private def createRegularModuleService(): CommonModuleService = {
    instance.stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of regular module without state.")
        new StatelessCommonModuleService(manager, taskInputService.checkpointGroup, performanceMetrics)
      case EngineLiterals.ramStateMode =>
        new StatefulCommonModuleService(manager, taskInputService.checkpointGroup, performanceMetrics)
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
      val maybeEnvelope = blockingQueue.get(instance.eventWaitIdleTime)

      maybeEnvelope match {
        case Some(serializedEnvelope) => {
          val envelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope)
          registerEnvelope(envelope)
          logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler.")
          executor.onMessage(envelope)
        }
        case None => {
          performanceMetrics.increaseTotalIdleTime(instance.eventWaitIdleTime)
          executor.onIdle()
        }
      }

      if (isItTimeToCheckpoint(moduleService.isCheckpointInitiated)) doCheckpoint()

      if (moduleTimer.isTime) {
        logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler.")
        executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
        moduleTimer.reset()
      }
    }
  }

  private def registerEnvelope(envelope: Envelope) = {
    afterReceivingEnvelope()
    taskInputService.registerEnvelope(envelope)
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

  protected def afterReceivingEnvelope(): Unit

  /**
   * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside regular module (not on the schedule) or not.
   */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  private def doCheckpoint() = {
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
  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates RegularTaskEngine is in charge of a basic execution logic of task of regular module
   * @return Engine of regular task
   */
  def apply(manager: CommonTaskManager,
                               performanceMetrics: RegularStreamingPerformanceMetrics): RegularTaskEngine = {
    val regularInstance = manager.instance.asInstanceOf[RegularInstance]

    regularInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.info(s"Task: ${manager.taskName}. Regular module has a '${EngineLiterals.timeIntervalMode}' checkpoint mode, create an appropriate task engine.")
        new RegularTaskEngine(manager, performanceMetrics) with TimeCheckpointTaskEngine
      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}. Regular module has an '${EngineLiterals.everyNthMode}' checkpoint mode, create an appropriate task engine.")
        new RegularTaskEngine(manager, performanceMetrics) with NumericalCheckpointTaskEngine

    }
  }
}