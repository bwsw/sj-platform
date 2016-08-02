package com.bwsw.sj.engine.regular.task.engine

import java.util.concurrent.Callable

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.engine.input.RegularTaskInputServiceFactory
import com.bwsw.sj.engine.regular.task.engine.state.{RegularTaskEngineService, StatelessRegularTaskEngineService, StatefulRegularTaskEngineService}
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.BasicProducer
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
abstract class RegularTaskEngine(manager: RegularTaskManager,
                                 performanceMetrics: RegularStreamingPerformanceMetrics,
                                 blockingQueue: PersistentBlockingQueue) extends Callable[Unit] {

  protected val currentThread = Thread.currentThread()
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] = manager.outputProducers
  protected val checkpointGroup = new CheckpointGroup()
  protected val regularInstance = manager.getInstanceMetadata.asInstanceOf[RegularInstance]
  protected val regularTaskEngineService = createRegularTaskEngineService()
  private val moduleEnvironmentManager = regularTaskEngineService.moduleEnvironmentManager
  protected val executor: RegularStreamingExecutor = regularTaskEngineService.executor
  private val moduleTimer = regularTaskEngineService.moduleTimer
  protected val outputTags = regularTaskEngineService.outputTags
  private val regularTaskInputServiceFactory = new RegularTaskInputServiceFactory(manager, blockingQueue, checkpointGroup)
  val regularTaskInputService = regularTaskInputServiceFactory.createRegularTaskInputService()
  protected val isNotOnlyCustomCheckpoint: Boolean

  /**
   * Json serializer for deserialization of envelope
   */
  protected val serializer = new JsonSerializer()
  serializer.setIgnoreUnknown(true)

  addProducersToCheckpointGroup()

  /**
   * It is responsible for handling an event about an envelope has been received
   */
  protected def afterReceivingEnvelope() = {}

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
    checkpointGroup.commit()
    outputTags.clear()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
    executor.onAfterCheckpoint()
  }

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
      val maybeEnvelope = blockingQueue.get(regularInstance.eventWaitTime)

      if (maybeEnvelope == null) {
        logger.debug(s"Task: ${manager.taskName}. Idle timeout: ${regularInstance.eventWaitTime} went out and nothing was received\n")
        logger.debug(s"Task: ${manager.taskName}. Increase total idle time\n")
        performanceMetrics.increaseTotalIdleTime(regularInstance.eventWaitTime)
        logger.debug(s"Task: ${manager.taskName}. Invoke onIdle() handler\n")
        executor.onIdle()
      } else {
        val envelope = serializer.deserialize[Envelope](maybeEnvelope)
        afterReceivingEnvelope()
        regularTaskInputService.processEnvelope(envelope, performanceMetrics)
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


  /**
   * Creates a manager of environment of regular streaming module
   * @return Manager of environment of regular streaming module
   */
  protected def createRegularTaskEngineService(): RegularTaskEngineService = {
    regularInstance.stateManagement match {
      case "none" =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of regular module without state\n")
        new StatelessRegularTaskEngineService(manager, performanceMetrics)
      case "ram" =>
        new StatefulRegularTaskEngineService(manager, checkpointGroup, performanceMetrics)
    }
  }

  /**
   * Adds producers for each output to checkpoint group
   */
  private def addProducersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    producers.foreach(x => checkpointGroup.add(x._2.name, x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")
  }
}
