package com.bwsw.sj.engine.regular.task.engine

import java.util.concurrent.Callable

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.engine.input.RegularTaskInputServiceFactory
import com.bwsw.sj.engine.regular.task.engine.state.{RegularTaskEngineService, StatefulRegularTaskEngineService, StatelessRegularTaskEngineService}
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
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 * @author Kseniya Mikhaleva
 */
abstract class RegularTaskEngine(protected val manager: RegularTaskManager,
                                 performanceMetrics: RegularStreamingPerformanceMetrics,
                                 blockingQueue: PersistentBlockingQueue) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"regular-task-${manager.taskName}-engine")
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val producers: Map[String, Producer[Array[Byte]]] = manager.outputProducers
  protected val checkpointGroup = new CheckpointGroup()
  protected val instance = manager.instance.asInstanceOf[RegularInstance]
  protected val regularTaskEngineService = createRegularTaskEngineService()
  protected val environmentManager = regularTaskEngineService.regularEnvironmentManager
  protected val executor = regularTaskEngineService.executor
  private val moduleTimer = regularTaskEngineService.moduleTimer
  protected val outputTags = regularTaskEngineService.outputTags
  private val regularTaskInputServiceFactory = new RegularTaskInputServiceFactory(manager, blockingQueue, checkpointGroup)
  val taskInputService = regularTaskInputServiceFactory.createRegularTaskInputService()
  protected val isNotOnlyCustomCheckpoint: Boolean

  protected val envelopeSerializer = new JsonSerializer(true)

  addProducersToCheckpointGroup()

  protected def createRegularTaskEngineService(): RegularTaskEngineService = {
    instance.stateManagement match {
      case "none" =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of regular module without state\n")
        new StatelessRegularTaskEngineService(manager, performanceMetrics)
      case "ram" =>
        new StatefulRegularTaskEngineService(manager, checkpointGroup, performanceMetrics)
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

      if (maybeEnvelope == null) {
        performanceMetrics.increaseTotalIdleTime(instance.eventWaitTime)
        executor.onIdle()
      } else {
        val envelope = envelopeSerializer.deserialize[Envelope](maybeEnvelope)
        afterReceivingEnvelope()
        taskInputService.registerEnvelope(envelope, performanceMetrics)
        logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
        executor.onMessage(envelope)
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
  protected def doCheckpoint() = {
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
