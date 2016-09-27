package com.bwsw.sj.engine.input.task.engine

import java.util.concurrent.{ArrayBlockingQueue, Callable}

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.input.eviction_policy.{ExpandedTimeEvictionPolicy, FixTimeEvictionPolicy}
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
abstract class InputTaskEngine(protected val manager: TaskManager,
                               performanceMetrics: InputStreamingPerformanceMetrics,
                               channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                               bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf]) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"input-task-${manager.taskName}-engine")
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val producers: Map[String, Producer[Array[Byte]]] = manager.outputProducers
  protected val streams = producers.keySet
  protected var transactionsByStreamPartitions = createTxnsStorage(streams)
  protected val checkpointGroup = new CheckpointGroup()
  protected val instance = manager.instance.asInstanceOf[InputInstance]
  protected val environmentManager = createModuleEnvironmentManager()
  private val executor = manager.getExecutor(environmentManager).asInstanceOf[InputStreamingExecutor]
  protected val evictionPolicy = createEvictionPolicy()
  protected val isNotOnlyCustomCheckpoint: Boolean

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

  /**
   * Creates an eviction policy that defines a way of eviction of duplicate envelope
   * @return Eviction policy of duplicate envelopes
   */
  private def createEvictionPolicy() = {
    instance.evictionPolicy match {
      case EngineLiterals.fixTimeEvictionPolicy => new FixTimeEvictionPolicy(instance)
      case EngineLiterals.expandedTimeEvictionPolicy => new ExpandedTimeEvictionPolicy(instance)
      case _ => new FixTimeEvictionPolicy(instance)
    }
  }

  private def addProducersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    producers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")
  }


  /**
   * Sends an input envelope to output steam
   */
  protected def sendEnvelope(stream: String, partition: Int, data: Array[Byte]) = {
    logger.info(s"Task name: ${manager.taskName}. Send envelope to each output stream.\n")
    val maybeTxn = getTxn(stream, partition)
    var transaction: ProducerTransaction[Array[Byte]] = null
    if (maybeTxn.isDefined) {
      logger.debug(s"Task name: ${manager.taskName}. Txn for stream/partition: '$stream/$partition' is defined\n")
      transaction = maybeTxn.get
      transaction.send(data)
    } else {
      logger.debug(s"Task name: ${manager.taskName}. Txn for stream/partition: '$stream/$partition' is not defined " +
        s"so create new txn\n")
      transaction = producers(stream).newTransaction(NewTransactionProducerPolicy.ErrorIfOpened, partition)
      transaction.send(data)
      putTxn(stream, partition, transaction)
    }

    logger.debug(s"Task name: ${manager.taskName}. Add envelope to output stream in performance metrics \n")
    performanceMetrics.addElementToOutputEnvelope(
      stream,
      transaction.getTransactionID().toString,
      data.length
    )
  }

  /**
   * It is in charge of running a basic execution logic of input task engine
   */
  def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run input task engine in a separate thread of execution service\n")
    while (true) {
      var channelContext = channelContextQueue.poll()
      if (channelContext == null) channelContext = getCtxOfNonEmptyBuffer()
      if (channelContext != null) {
        setOfContexts.add(channelContext)
        logger.debug(s"Task name: ${manager.taskName}. Invoke tokenize() method of executor\n")
        val buffer = bufferForEachContext(channelContext)
        val maybeInterval = executor.tokenize(buffer)
        if (maybeInterval.isDefined) {
          logger.debug(s"Task name: ${manager.taskName}. Tokenize() method returned a defined interval\n")
          val interval = maybeInterval.get
          if (buffer.isReadable(interval.finalValue)) {
            logger.debug(s"Task name: ${manager.taskName}. The end index of interval is valid\n")
            logger.debug(s"Task name: ${manager.taskName}. Invoke parse() method of executor\n")
            val inputEnvelope: Option[InputEnvelope] = executor.parse(buffer, interval)
            clearBufferAfterTokenize(buffer, interval.finalValue)
            val isNotDuplicateOrEmpty = processEnvelope(inputEnvelope)
            envelopeProcessed(inputEnvelope, isNotDuplicateOrEmpty, channelContext)
          } else {
            logger.error(s"Task name: ${manager.taskName}. " +
              s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
              s"that an input stream is not defined at (buffer write index: ${buffer.writerIndex()})\n")
            throw new IndexOutOfBoundsException(s"Task name: ${manager.taskName}. " +
              s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
              s"that an input stream is not defined at (buffer write index: ${buffer.writerIndex()})")
          }
        }
      }

      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }

  private def getCtxOfNonEmptyBuffer() = {
    val maybeCtx = bufferForEachContext.find(x => x._2.readableBytes() > 0)
    if (maybeCtx.isDefined) maybeCtx.get._1 else null
  }

  /**
   * Removes the read bytes of the byte buffer
   * @param buffer A buffer for keeping incoming bytes
   * @param endReadingIndex Index that was last at reading
   */
  private def clearBufferAfterTokenize(buffer: ByteBuf, endReadingIndex: Int) = {
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
  protected def processEnvelope(envelope: Option[InputEnvelope]): Boolean = {
    if (envelope.isDefined) {
      logger.info(s"Task name: ${manager.taskName}. Envelope is defined. Process it\n")
      val inputEnvelope = envelope.get
      logger.debug(s"Task name: ${manager.taskName}. Add envelope to input stream in performance metrics \n")
      performanceMetrics.addEnvelopeToInputStream(List(inputEnvelope.data.length))
      if (checkForDuplication(inputEnvelope.key, inputEnvelope.duplicateCheck, inputEnvelope.data)) {
        logger.debug(s"Task name: ${manager.taskName}. Envelope is not duplicate so send it\n")
        inputEnvelope.outputMetadata.foreach(x => {
          sendEnvelope(x._1, x._2, inputEnvelope.data)
        })
        afterReceivingEnvelope()
        return true
      }
    }
    logger.debug(s"Task name: ${manager.taskName}. Envelope hasn't been processed\n")
    false
  }

  /**
   * Checks whether a key is duplicate or not if it's necessary
   * @param key Key for checking
   * @param duplicateCheck Flag points a key has to be checked or not.
   * @param value In case there is a need to update duplicate key this value will be used
   * @return True if a processed envelope is not duplicate and false in other case
   */
  protected def checkForDuplication(key: String, duplicateCheck: Boolean, value: Array[Byte]): Boolean = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Try to check key: '$key' for duplication with a setting duplicateCheck = '$duplicateCheck' " +
      s"and an instance setting - 'duplicate-check' : '${instance.duplicateCheck}'\n")
    if (instance.duplicateCheck || duplicateCheck) {
      logger.info(s"Task name: ${manager.taskName}. " +
        s"Check key: '$key' for duplication\n")
      evictionPolicy.checkForDuplication(key, value)
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
  protected def envelopeProcessed(envelope: Option[InputEnvelope], isNotEmptyOrDuplicate: Boolean, ctx: ChannelHandlerContext) = {
    val inputStreamingResponse = executor.createProcessedMessageResponse(envelope, isNotEmptyOrDuplicate)
    if (inputStreamingResponse.isBuffered) ctx.write(inputStreamingResponse.message)
    else ctx.writeAndFlush(inputStreamingResponse.message)
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  protected def doCheckpoint(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
    checkpointGroup.checkpoint()
    checkpointInitiated()
    transactionsByStreamPartitions = createTxnsStorage(streams)
    prepareForNextCheckpoint()
  }

  /**
   * It is responsible for sending a response to client about the fact that a checkpoint has been done
   * It will be invoked after commit of checkpoint group
   */
  protected def checkpointInitiated() = {
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
  private def getTxn(stream: String, partition: Int) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Try to get txn by stream: $stream, partition: $partition\n")
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
      s"Put txn for stream: $stream, partition: $partition\n")
    transactionsByStreamPartitions(stream) += (partition -> transaction)
  }

  /**
   * Creates a map that keeps current open txn for each partition of output stream
   * @param streams Set of names of output streams
   * @return Map where a key is output stream name, a value is a map
   *         in which key is a number of partition and value is a txn
   */
  protected def createTxnsStorage(streams: Set[String]) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Create storage for keeping txns for each partition of output streams\n")
    streams.map(x => (x, mutable.Map[Int, ProducerTransaction[Array[Byte]]]())).toMap
  }
}