package com.bwsw.sj.engine.output.task

import java.util.concurrent.Callable

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.input.TStreamTaskInputService
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, PersistentBlockingQueue}
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.output.processing.OutputProcessor
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory


/**
 * Provided methods are responsible for a basic execution logic of task of output module
 *
 * @param manager Manager of environment of task of output module
 * @param performanceMetrics Set of metrics that characterize performance of a output streaming module

 * @author Kseniya Mikhaleva
 */
abstract class OutputTaskEngine(protected val manager: OutputTaskManager,
                                performanceMetrics: OutputStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"output-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(EngineLiterals.persistentBlockingQueue)
  private val envelopeSerializer = new JsonSerializer()
  private val instance = manager.instance.asInstanceOf[OutputInstance]
  private val outputStream = getOutputStream
  private val environmentManager = createModuleEnvironmentManager()
  private val executor = manager.getExecutor(environmentManager).asInstanceOf[OutputStreamingExecutor]
  val taskInputService = new TStreamTaskInputService(manager, blockingQueue)
  private val outputProcessor = OutputProcessor(outputStream, performanceMetrics, manager)
  private var wasFirstCheckpoint = false
  protected val checkpointInterval = instance.checkpointInterval

  private def getOutputStream: SjStream = {
    val streamService = ConnectionRepository.getStreamService
    instance.outputs.flatMap(x => streamService.get(x)).head
  }

  private def createModuleEnvironmentManager() = {
    val streamService = ConnectionRepository.getStreamService
    val outputs = instance.outputs
      .flatMap(x => streamService.get(x))
    val options = instance.getOptionsAsMap()
    new OutputEnvironmentManager(options, outputs)
  }

  /**
   * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
   *
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside output module (not on the schedule) or not.
   */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean


  /**
   * It is in charge of running a basic execution logic of output task engine
   */
  override def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run output task engine in a separate thread of execution service\n")

    while (true) {
      val maybeEnvelope = blockingQueue.get(EngineLiterals.eventWaitTimeout)

      maybeEnvelope match {
        case Some(serializedEnvelope) =>
          processOutputEnvelope(serializedEnvelope)
        case _ =>
      }
      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }


  /**
   * Handler for sending data to storage.
   *
   * @param serializedEnvelope: original envelope.
   */
  private def processOutputEnvelope(serializedEnvelope: String) = {
    afterReceivingEnvelope()
    val inputEnvelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope).asInstanceOf[TStreamEnvelope]
    registerInputEnvelope(inputEnvelope)
    logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
    val outputEnvelopes: List[Envelope] = executor.onMessage(inputEnvelope)
    outputProcessor.process(outputEnvelopes, inputEnvelope, wasFirstCheckpoint)
  }


  /**
   * Register received envelope in performance metrics.
   *
   * @param envelope: received data
   */
  private def registerInputEnvelope(envelope: Envelope) = {
    taskInputService.registerEnvelope(envelope)
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }


  /**
   * Doing smth after catch envelope.
   */
  protected def afterReceivingEnvelope(): Unit


  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  protected def doCheckpoint() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
    taskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
    prepareForNextCheckpoint()
    wasFirstCheckpoint = true
  }

  protected def prepareForNextCheckpoint(): Unit

}

object OutputTaskEngine {
  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates OutputTaskEngine is in charge of a basic execution logic of task of output module
   * @return Engine of output task
   */
  def apply(manager: OutputTaskManager,
            performanceMetrics: OutputStreamingPerformanceMetrics): OutputTaskEngine = {

    manager.outputInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.error(s"Task: ${manager.taskName}. Output module can't have a '${EngineLiterals.timeIntervalMode}' checkpoint mode\n")
        throw new Exception(s"Task: ${manager.taskName}. Output module can't have a '${EngineLiterals.timeIntervalMode}' checkpoint mode\n")
      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}. Output module has an '${EngineLiterals.everyNthMode}' checkpoint mode, create an appropriate task engine\n")
        new OutputTaskEngine(manager, performanceMetrics) with NumericalCheckpointTaskEngine
    }
  }
}