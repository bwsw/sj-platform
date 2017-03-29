package com.bwsw.sj.engine.input.task

import java.util.concurrent.{ArrayBlockingQueue, Callable, TimeUnit}

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.Interval
import com.bwsw.sj.engine.input.eviction_policy.InputInstanceEvictionPolicy
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer, ProducerTransaction}
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory

import scala.collection._

/**
 * Provides methods are responsible for a basic execution logic of task of input module
 *
 *
 * @param manager Manager of environment of task of input module
 * @param performanceMetrics Set of metrics that characterize performance of a input streaming module
 * @param channelContextQueue Queue for keeping a channel context to process messages (byte buffer) in their turn
 * @param bufferForEachContext Map for keeping a buffer containing incoming bytes with the channel context
 * @author Kseniya Mikhaleva
 */
abstract class InputTaskEngine(protected val manager: InputTaskManager,
                               performanceMetrics: InputStreamingPerformanceMetrics,
                               channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                               bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf]) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"input-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val producers: Map[String, Producer[Array[Byte]]] = manager.outputProducers
  private var transactionsByStreamPartitions = createTxnsStorage()
  private val checkpointGroup = new CheckpointGroup()
  private val instance = manager.instance.asInstanceOf[InputInstance]
  private val environmentManager = createModuleEnvironmentManager()
  private val executor = manager.getExecutor(environmentManager)
  private val evictionPolicy = InputInstanceEvictionPolicy(instance)
  protected val checkpointInterval = instance.checkpointInterval

  /**
   * Set of channel contexts related with the input envelopes to send a message about an event
   * that a checkpoint has been initiated,
   */
  private val setOfContexts = collection.mutable.Set[ChannelHandlerContext]()

  addProducersToCheckpointGroup()

  /**
   * Creates a manager of environment of input streaming module
   * @return Manager of environment of input streaming module
   */
  private def createModuleEnvironmentManager() = {
    val options = instance.getOptionsAsMap()
    val streamService = ConnectionRepository.getStreamService
    val taggedOutputs = instance.outputs
      .flatMap(x => streamService.get(x))

    new InputEnvironmentManager(options, taggedOutputs)
  }

  private def addProducersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group.")
    producers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group.")
  }

  /**
   * Sends an input envelope data to output steam
   */
  private def sendData(stream: String, partition: Int, data: AnyRef) = {
    logger.info(s"Task name: ${manager.taskName}. Send envelope to each output stream..")
    val maybeTransaction = getTransaction(stream, partition)
    val bytes = manager.envelopeDataSerializer.serialize(data)
    val producerTransaction = maybeTransaction match {
      case Some(transaction) =>
        logger.debug(s"Task name: ${manager.taskName}. Txn for stream/partition: '$stream/$partition' is defined.")
        transaction.send(bytes)

        transaction
      case None =>
        logger.debug(s"Task name: ${manager.taskName}. Txn for stream/partition: '$stream/$partition' is not defined " +
          s"so create new txn.")
        val transaction = producers(stream).newTransaction(NewTransactionProducerPolicy.ErrorIfOpened, partition)
        transaction.send(bytes)
        putTxn(stream, partition, transaction)

        transaction
    }

    logger.debug(s"Task name: ${manager.taskName}. Add envelope to output stream in performance metrics .")
    performanceMetrics.addElementToOutputEnvelope(
      stream,
      producerTransaction.getTransactionID().toString,
      bytes.length
    )
  }

  /**
   * It is in charge of running a basic execution logic of input task engine
   */
  def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run input task engine in a separate thread of execution service.")
    while (true) {
      getChannelContext() match {
        case Some(channelContext) =>
          setOfContexts.add(channelContext)
          logger.debug(s"Task name: ${manager.taskName}. Invoke tokenize() method of executor.")
          val maybeInterval = executor.tokenize(bufferForEachContext(channelContext))
          if (maybeInterval.isDefined) {
            logger.debug(s"Task name: ${manager.taskName}. Tokenize() method returned a defined interval.")
            tryToRead(channelContext, maybeInterval.get)
          }
        case None =>
      }

      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }

  private def getChannelContext() = {
    var channelContext = channelContextQueue.poll(EngineLiterals.eventWaitTimeout, TimeUnit.MILLISECONDS)
    if (channelContext == null) channelContext = getCtxOfNonEmptyBuffer()

    Option(channelContext)
  }

  private def getCtxOfNonEmptyBuffer() = {
    val maybeCtx = bufferForEachContext.find(x => x._2.readableBytes() > 0)
    if (maybeCtx.isDefined) maybeCtx.get._1 else null
  }

  private def tryToRead(channelContext: ChannelHandlerContext, interval: Interval) = {
    val buffer = bufferForEachContext(channelContext)
    if (buffer.isReadable(interval.finalValue)) {
      logger.debug(s"Task name: ${manager.taskName}. The end index of interval is valid.")
      logger.debug(s"Task name: ${manager.taskName}. Invoke parse() method of executor.")
      val inputEnvelope = executor.parse(buffer, interval)
      clearBufferAfterParse(buffer, interval.finalValue)
      val isNotDuplicateOrEmpty = processEnvelope(inputEnvelope)
      sendClientResponse(inputEnvelope, isNotDuplicateOrEmpty, channelContext)
    } else {
      logger.error(s"Task name: ${manager.taskName}. " +
        s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
        s"that an input stream is not defined at (buffer write index: ${buffer.writerIndex()}).")
      throw new IndexOutOfBoundsException(s"Task name: ${manager.taskName}. " +
        s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
        s"that an input stream is not defined at (buffer write index: ${buffer.writerIndex()})")
    }
  }

  /**
   * Removes the read bytes of the byte buffer
   * @param buffer A buffer for keeping incoming bytes
   * @param endReadingIndex Index that was last at reading
   */
  private def clearBufferAfterParse(buffer: ByteBuf, endReadingIndex: Int) = {
    buffer.readerIndex(endReadingIndex + 1)
    buffer.discardReadBytes()
  }

  /**
   * It is responsible for processing of envelope:
   * 1) checks whether an input envelope is defined and isn't duplicate or not
   * 2) if (1) is true an input envelope is sent to output stream(s)
   * @param envelope May be input envelope
   * @return True if a processed envelope is processed, e.i. it is not duplicate or empty, and false in other case
   */
  private def processEnvelope(envelope: Option[InputEnvelope[AnyRef]]): Boolean = {
    envelope match {
      case Some(inputEnvelope) =>
        logger.info(s"Task name: ${manager.taskName}. Envelope is defined. Process it.")
        performanceMetrics.addEnvelopeToInputStream(inputEnvelope)
        if (checkForDuplication(inputEnvelope.key, inputEnvelope.duplicateCheck)) {
          logger.debug(s"Task name: ${manager.taskName}. Envelope is not duplicate so send it.")
          inputEnvelope.outputMetadata.foreach(x => {
            sendData(x._1, x._2, inputEnvelope.data)
          })
          afterReceivingEnvelope()
          true
        } else false
      case None => false
    }
  }

  /**
   * Checks whether a key is duplicate or not if it's necessary
   * @param key Key for checking
   * @param duplicateCheck Flag points a key has to be checked or not.
   * @return True if a processed envelope is not duplicate and false in other case
   */
  private def checkForDuplication(key: String, duplicateCheck: Boolean): Boolean = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Try to check key: '$key' for duplication with a setting duplicateCheck = '$duplicateCheck' " +
      s"and an instance setting - 'duplicate-check' : '${instance.duplicateCheck}'.")
    if (instance.duplicateCheck || duplicateCheck) {
      logger.info(s"Task name: ${manager.taskName}. " +
        s"Check key: '$key' for duplication.")
      evictionPolicy.checkForDuplication(key)
    } else true
  }

  protected def afterReceivingEnvelope(): Unit

  /**
   * It is responsible for sending an answer to client about the fact that an envelope is processed
   * @param envelope Input envelope
   * @param isNotEmptyOrDuplicate Flag points whether a processed envelope is empty or duplicate  or not.
   *                              If it is true it means a processed envelope is duplicate or empty and false in other case
   * @param ctx Channel context related with this input envelope to send a message about this event
   */
  private def sendClientResponse(envelope: Option[InputEnvelope[AnyRef]], isNotEmptyOrDuplicate: Boolean, ctx: ChannelHandlerContext) = {
    val inputStreamingResponse = executor.createProcessedMessageResponse(envelope, isNotEmptyOrDuplicate)
    if (inputStreamingResponse.isBuffered) ctx.write(inputStreamingResponse.message)
    else ctx.writeAndFlush(inputStreamingResponse.message)
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  private def doCheckpoint(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint.")
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint.")
    checkpointGroup.checkpoint()
    checkpointInitiated()
    transactionsByStreamPartitions = createTxnsStorage()
    prepareForNextCheckpoint()
  }

  /**
   * It is responsible for sending a response to client about the fact that a checkpoint has been done
   * It will be invoked after commit of checkpoint group
   */
  private def checkpointInitiated() = {
    val inputStreamingResponse = executor.createCheckpointResponse()
    if (inputStreamingResponse.isBuffered) setOfContexts.foreach(x => x.write(inputStreamingResponse.message))
    else setOfContexts.foreach(x => x.writeAndFlush(inputStreamingResponse.message))
  }

  protected def prepareForNextCheckpoint(): Unit

  /**
   * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside input module (not on the schedule) or not.
   */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean

  /**
   * Retrieves a txn for specific stream and partition
   * @param stream Output stream name
   * @param partition Partition of stream
   * @return Current open transaction
   */
  private def getTransaction(stream: String, partition: Int) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Try to get txn by stream: $stream, partition: $partition.")
    if (transactionsByStreamPartitions(stream).isDefinedAt(partition)) {
      Some(transactionsByStreamPartitions(stream)(partition))
    } else None
  }

  /**
   * Retrieves a txn for specific stream and partition
   * @param stream Output stream name
   * @param partition Partition of stream
   * @return Current open transaction
   */
  private def putTxn(stream: String, partition: Int, transaction: ProducerTransaction[Array[Byte]]) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Put txn for stream: $stream, partition: $partition.")
    transactionsByStreamPartitions(stream) += (partition -> transaction)
  }

  /**
   * Creates a map that keeps current open txn for each partition of output stream
   * @return Map where a key is output stream name, a value is a map
   *         in which key is a number of partition and value is a txn
   */
  private def createTxnsStorage() = {
    val streams = producers.keySet
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Create a storage for keeping txns for each partition of output streams.")
    streams.map(x => (x, mutable.Map[Int, ProducerTransaction[Array[Byte]]]())).toMap
  }
}

object InputTaskEngine {
  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates InputTaskEngine is in charge of a basic execution logic of task of input module
   * @return Engine of input task
   */
  def apply(manager: InputTaskManager,
            performanceMetrics: InputStreamingPerformanceMetrics,
            channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
            bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf]) = {

    manager.inputInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.info(s"Task: ${manager.taskName}. Input module has a '${EngineLiterals.timeIntervalMode}' checkpoint mode, create an appropriate task engine.")
        new InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext) with TimeCheckpointTaskEngine
      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}. Input module has an '${EngineLiterals.everyNthMode}' checkpoint mode, create an appropriate task engine.")
        new InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext) with NumericalCheckpointTaskEngine
    }
  }
}