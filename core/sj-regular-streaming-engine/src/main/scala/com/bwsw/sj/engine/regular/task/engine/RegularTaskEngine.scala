package com.bwsw.sj.engine.regular.task.engine

import java.util.concurrent.Callable

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.{StatelessCommonModuleService, StatefulCommonModuleService, CommonModuleService}
import com.bwsw.sj.engine.regular.task.engine.input.RegularTaskInputServiceFactory
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
  private val taskInputServiceFactory = new RegularTaskInputServiceFactory(manager, blockingQueue)
  val taskInputService = taskInputServiceFactory.createTaskInputService()
  private val moduleService = createRegularModuleService()
  private val executor = moduleService.executor.asInstanceOf[RegularStreamingExecutor]
  private val moduleTimer = moduleService.moduleTimer
  protected val isNotOnlyCustomCheckpoint: Boolean

  private val envelopeSerializer = new JsonSerializer(true)

  private def createRegularModuleService(): CommonModuleService = {
    instance.stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of regular module without state\n")
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
      s"Run regular task engine in a separate thread of execution service\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onInit() handler\n")
    executor.onInit()

    while (true) {
      val maybeEnvelope = blockingQueue.get(instance.eventWaitTime)

      maybeEnvelope match {
        case Some(serializedEnvelope) => {
          val envelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope)
          afterReceivingEnvelope()
          taskInputService.registerEnvelope(envelope, performanceMetrics)
          logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
          executor.onMessage(envelope)
        }
        case None => {
          performanceMetrics.increaseTotalIdleTime(instance.eventWaitTime)
          executor.onIdle()
        }
      }

      if (isItTimeToCheckpoint(moduleService.isCheckpointInitiated)) doCheckpoint()

      if (moduleTimer.isTime) {
        logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler\n")
        executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
        moduleTimer.reset()
      }
    }
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
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler\n")
    executor.onBeforeCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
    moduleService.doCheckpoint()
    taskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
    executor.onAfterCheckpoint()
    prepareForNextCheckpoint()
  }

  protected def prepareForNextCheckpoint(): Unit
}
