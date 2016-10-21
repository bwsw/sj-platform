package com.bwsw.sj.engine.regular.task.engine

import java.util.concurrent.Callable

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.engine.input.RegularTaskInputServiceFactory
import com.bwsw.sj.engine.regular.task.engine.state.{StatelessRegularModuleService, StatefulRegularModuleService, RegularModuleService}
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.Producer
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
 * Provides methods are responsible for a basic execution logic of task of regular module
 *
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module

 * @author Kseniya Mikhaleva
 */
abstract class RegularTaskEngine(protected val manager: RegularTaskManager,
                                 performanceMetrics: RegularStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"regular-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(EngineLiterals.persistentBlockingQueue)
  private val producers: Map[String, Producer[Array[Byte]]] = manager.outputProducers
  private val checkpointGroup = new CheckpointGroup()
  private val instance = manager.instance.asInstanceOf[RegularInstance]
  private val regularTaskEngineService = createRegularTaskEngineService()
  private val environmentManager = regularTaskEngineService.regularEnvironmentManager
  private val executor = regularTaskEngineService.executor
  private val moduleTimer = regularTaskEngineService.moduleTimer
  private val outputTags = regularTaskEngineService.outputTags
  private val regularTaskInputServiceFactory = new RegularTaskInputServiceFactory(manager, blockingQueue, checkpointGroup)
  val taskInputService = regularTaskInputServiceFactory.createRegularTaskInputService()
  protected val isNotOnlyCustomCheckpoint: Boolean

  private val envelopeSerializer = new JsonSerializer(true)

  addProducersToCheckpointGroup()

  private def createRegularTaskEngineService(): RegularModuleService = {
    instance.stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of regular module without state\n")
        new StatelessRegularModuleService(manager, checkpointGroup, performanceMetrics)
      case EngineLiterals.ramStateMode =>
        new StatefulRegularModuleService(manager, checkpointGroup, performanceMetrics)
    }
  }

  private def addProducersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    producers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")
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

      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()

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
    regularTaskEngineService.doCheckpoint()
    taskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
    checkpointGroup.checkpoint()
    outputTags.clear()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
    executor.onAfterCheckpoint()
    prepareForNextCheckpoint()
  }

  protected def prepareForNextCheckpoint(): Unit
}
