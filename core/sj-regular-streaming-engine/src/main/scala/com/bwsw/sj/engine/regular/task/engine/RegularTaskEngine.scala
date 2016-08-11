package com.bwsw.sj.engine.regular.task.engine

import java.util.concurrent.Callable

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.engine.core.PersistentBlockingQueue
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
 * Created: 26/07/2016
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

  protected val currentThread = Thread.currentThread()
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val producers: Map[String, Producer[Array[Byte]]] = manager.outputProducers
  protected val checkpointGroup = new CheckpointGroup()
  protected val instance = manager.getInstance.asInstanceOf[RegularInstance]
  protected val regularTaskEngineService = createRegularTaskEngineService()
  protected val moduleEnvironmentManager = regularTaskEngineService.moduleEnvironmentManager
  protected val executor = regularTaskEngineService.executor
  private val moduleTimer = regularTaskEngineService.moduleTimer
  protected val outputTags = regularTaskEngineService.outputTags
  private val regularTaskInputServiceFactory = new RegularTaskInputServiceFactory(manager, blockingQueue, checkpointGroup)
  val regularTaskInputService = regularTaskInputServiceFactory.createRegularTaskInputService()
  protected val isNotOnlyCustomCheckpoint: Boolean

  protected val envelopeSerializer = new JsonSerializer()
  envelopeSerializer.setIgnoreUnknown(true)

  addProducersToCheckpointGroup()

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  protected def doCheckpoint() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler\n")
    executor.onBeforeCheckpoint()
    regularTaskEngineService.doCheckpoint()
    regularTaskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
    checkpointGroup.checkpoint()
    outputTags.clear()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
    executor.onAfterCheckpoint()
    prepareForNextCheckpoint()
  }

  protected def prepareForNextCheckpoint(): Unit

  /**
   * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside regular module (not on the schedule) or not.
   */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean

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
        logger.debug(s"Task: ${manager.taskName}. Idle timeout: ${instance.eventWaitTime} went out and nothing was received\n")
        logger.debug(s"Task: ${manager.taskName}. Increase total idle time\n")
        performanceMetrics.increaseTotalIdleTime(instance.eventWaitTime)
        logger.debug(s"Task: ${manager.taskName}. Invoke onIdle() handler\n")
        executor.onIdle()
      } else {
        val envelope = envelopeSerializer.deserialize[Envelope](maybeEnvelope)
        afterReceivingEnvelope()
        regularTaskInputService.registerEnvelope(envelope, performanceMetrics)
        logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
        executor.onMessage(envelope)
      }

      if (isItTimeToCheckpoint(moduleEnvironmentManager.isCheckpointInitiated)) doCheckpoint()

      if (moduleTimer.isTime) {
        logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler\n")
        executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
        moduleTimer.reset()
      }
    }
  }

  protected def afterReceivingEnvelope(): Unit

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
}
