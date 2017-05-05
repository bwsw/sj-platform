package com.bwsw.sj.engine.regular.task

import java.util.concurrent.{ArrayBlockingQueue, Callable, TimeUnit}

import com.bwsw.sj.common._dal.model.module.RegularInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.input.CallableCheckpointTaskInput
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.{CommonModuleService, StatefulCommonModuleService, StatelessCommonModuleService}
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

/**
  * Provides methods are responsible for a basic execution logic of task of regular module
  *
  * @param manager            Manager of environment of task of regular module
  * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
  * @author Kseniya Mikhaleva
  */
abstract class RegularTaskEngine(protected val manager: CommonTaskManager,
                                 performanceMetrics: RegularStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"regular-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val blockingQueue = new ArrayBlockingQueue[Envelope](EngineLiterals.queueSize)
  private val instance = manager.instance.asInstanceOf[RegularInstance]
  private val checkpointGroup = new CheckpointGroup()
  private val moduleService = createRegularModuleService()
  private val executor = moduleService.executor.asInstanceOf[RegularStreamingExecutor[AnyRef]]
  val taskInputService = CallableCheckpointTaskInput[AnyRef](manager, blockingQueue, checkpointGroup).asInstanceOf[CallableCheckpointTaskInput[Envelope]]
  private val moduleTimer = moduleService.moduleTimer
  protected val checkpointInterval = instance.checkpointInterval

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
        case Some(envelope) => {
          registerEnvelope(envelope)
          logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler.")
          envelope match {
            case kafkaEnvelope: KafkaEnvelope[AnyRef@unchecked] =>
              executor.onMessage(kafkaEnvelope)
            case tstreamEnvelope: TStreamEnvelope[AnyRef@unchecked] =>
              executor.onMessage(tstreamEnvelope)
            case wrongEnvelope =>
              logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/batch streaming engine")
              throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined for regular/batch streaming engine")
          }
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
    *
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
    *
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