package com.bwsw.sj.engine.output.task.engine

import java.util.concurrent.Callable

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.TStreamTaskInputService
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.engine.Handler.{EsOutputHandler, JdbcOutputHandler, OutputHandler}
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
  protected val environmentManager = createModuleEnvironmentManager()
  private val executor = manager.getExecutor(environmentManager).asInstanceOf[OutputStreamingExecutor]
  val taskInputService = new TStreamTaskInputService(manager, blockingQueue)
  protected val isNotOnlyCustomCheckpoint: Boolean
  private val handler = createHandler(outputStream)
  private var wasFirstCheckpoint = false



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


  def createHandler(outputStream: SjStream): OutputHandler = {
    outputStream.streamType match {
      case StreamLiterals.esOutputType =>
        new EsOutputHandler(outputStream, performanceMetrics, manager)
      case StreamLiterals.jdbcOutputType =>
        new JdbcOutputHandler(outputStream, performanceMetrics, manager)
    }
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
    * Handler of envelope.
    *
    * @param serializedEnvelope: original envelope.
    */
  private def processOutputEnvelope(serializedEnvelope: String) = {
// todo    afterReceivingEnvelope()
    val envelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope).asInstanceOf[TStreamEnvelope]
    registerInputEnvelope(envelope)
    logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
    val outputEnvelopes: List[Envelope] = executor.onMessage(envelope)
    handler.process(outputEnvelopes, envelope, wasFirstCheckpoint)
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



// todo
//  /**
//    * Doing smth after catch envelope.
//    */
//  protected def afterReceivingEnvelope() = {}



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
