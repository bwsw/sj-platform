package com.bwsw.sj.engine.input.task.engine

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import com.bwsw.sj.engine.input.eviction_policy.{ExpandedTimeEvictionPolicy, FixTimeEvictionPolicy}
import com.bwsw.sj.engine.input.task.InputTaskManager
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerTransaction, ProducerPolicies}
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory

import scala.collection._

/**
 * Provides methods are responsible for a basic execution logic of task of input module
 * Created: 10/07/2016
 *
 * @param manager Manager of environment of task of input module
 * @param performanceMetrics Set of metrics that characterize performance of a input streaming module
 * @param channelContextQueue Queue for keeping a channel context to process messages (byte buffer) in their turn
 * @param bufferForEachContext Map for keeping a buffer containing incoming bytes with the channel context
 * @author Kseniya Mikhaleva
 */
abstract class InputTaskEngine(manager: InputTaskManager,
                               performanceMetrics: InputStreamingPerformanceMetrics,
                               channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                               bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf]) extends Runnable {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] = manager.createOutputProducers
  protected val streams = producers.keySet
  protected var txnsByStreamPartitions = createTxnsStorage(streams)
  protected val checkpointGroup = new CheckpointGroup()
  protected val inputInstance = manager.getInstanceMetadata.asInstanceOf[InputInstance]
  protected val moduleEnvironmentManager = createModuleEnvironmentManager()
  val executor: InputStreamingExecutor = manager.getExecutor(moduleEnvironmentManager)
  protected val evictionPolicy = createEvictionPolicy()
  protected val isNotOnlyCustomCheckpoint: Boolean

  addProducersToCheckpointGroup()

  /**
   * It is responsible for sending a response to client about the fact that a checkpoint has been done
   * It will be invoked after commit of checkpoint group
   * @param ctx Channel context related with the input envelope,
   *            after which checkpoint has been initiated, to send a message about this event
   */
  protected def checkpointInitiated(ctx: ChannelHandlerContext) = {
    val inputStreamingResponse = executor.createCheckpointResponse()
    if (inputStreamingResponse.isBuffered) ctx.write(inputStreamingResponse.message)
    else ctx.writeAndFlush(inputStreamingResponse.message)
  }

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
        return true
      }
    }
    logger.debug(s"Task name: ${manager.taskName}. Envelope hasn't been processed\n")
    false
  }

  /**
   * Sends an input envelope to output steam
   * @param stream Output stream name
   * @param partition Partition of stream
   * @param data Data for sending
   */
  protected def sendEnvelope(stream: String, partition: Int, data: Array[Byte]) = {
    logger.info(s"Task name: ${manager.taskName}. Send envelope to each output stream.\n")
    val maybeTxn = getTxn(stream, partition)
    var txn: BasicProducerTransaction[Array[Byte], Array[Byte]] = null
    if (maybeTxn.isDefined) {
      logger.debug(s"Task name: ${manager.taskName}. Txn for stream/partition: '$stream/$partition' is defined\n")
      txn = maybeTxn.get
      txn.send(data)
    } else {
      logger.debug(s"Task name: ${manager.taskName}. Txn for stream/partition: '$stream/$partition' is not defined " +
        s"so create new txn\n")
      txn = producers(stream).newTransaction(ProducerPolicies.errorIfOpened, partition)
      txn.send(data)
      putTxn(stream, partition, txn)
    }

    logger.debug(s"Task name: ${manager.taskName}. Add envelope to output stream in performance metrics \n")
    performanceMetrics.addElementToOutputEnvelope(
      stream,
      txn.getTxnUUID.toString,
      data.length
    )
  }

  /**
   * Checks whether a key is duplicate or not if it's necessary
   * @param key Key for checking
   * @param duplicateCheck Flag points a key has to be checked or not.
   * @param value In case there has to update duplicate key this value will be used
   * @return True if a processed envelope is not duplicate and false in other case
   */
  protected def checkForDuplication(key: String, duplicateCheck: Boolean, value: Array[Byte]): Boolean = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Try to check key: '$key' for duplication with a setting duplicateCheck = '$duplicateCheck' " +
      s"and an instance setting - 'duplicate-check' : '${inputInstance.duplicateCheck}'\n")
    if (inputInstance.duplicateCheck || duplicateCheck) {
      logger.info(s"Task name: ${manager.taskName}. " +
        s"Check key: '$key' for duplication\n")
      evictionPolicy.checkForDuplication(key, value)
    } else true
  }

  /**
   * It is in charge of running a basic execution logic of input task engine
   */
  override def run(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run input task engine in a separate thread of execution service\n")
    while (true) {
      var channelContext = channelContextQueue.poll()
      if (channelContext == null) channelContext = getCtxOfNonEmptyBuffer()
      if (channelContext != null) {
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
            doCheckpoint(moduleEnvironmentManager.isCheckpointInitiated, channelContext)
          } else {
            logger.error(s"Task name: ${manager.taskName}. " +
              s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
              s"that an input stream is not defined at (buffer write index: ${buffer.writerIndex()})\n")
            throw new IndexOutOfBoundsException(s"Task name: ${manager.taskName}. " +
              s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
              s"that an input stream is not defined at (buffer write index: ${buffer.writerIndex()})")
          }
        }
      } else logger.debug(s"Task name: ${manager.taskName}. Nothing to execute because of a message queue is empty")
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
   * Does group checkpoint of t-streams consumers/producers
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside input module (not on the schedule) or not.
   * @param ctx Channel context related with this input envelope to send a message about this event
   */
  protected def doCheckpoint(isCheckpointInitiated: Boolean, ctx: ChannelHandlerContext): Unit

  /**
   * Retrieves a txn for specific stream and partition
   * @param stream Output stream name
   * @param partition Partition of stream
   * @return Current open transaction
   */
  private def getTxn(stream: String, partition: Int) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Try to get txn by stream: $stream, partition: $partition\n")
    if (txnsByStreamPartitions(stream).isDefinedAt(partition)) {
      Some(txnsByStreamPartitions(stream)(partition))
    } else None
  }

  /**
   * Retrieves a txn for specific stream and partition
   * @param stream Output stream name
   * @param partition Partition of stream
   * @return Current open transaction
   */
  private def putTxn(stream: String, partition: Int, txn: BasicProducerTransaction[Array[Byte], Array[Byte]]) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Put txn for stream: $stream, partition: $partition\n")
    txnsByStreamPartitions(stream) += (partition -> txn)
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
    streams.map(x => (x, mutable.Map[Int, BasicProducerTransaction[Array[Byte], Array[Byte]]]())).toMap
  }

  /**
   * Adds producers for each output to checkpoint group
   */
  private def addProducersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    producers.foreach(x => checkpointGroup.add(x._2.name, x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")
  }

  /**
   * Creates a manager of environment of input streaming module
   * @return Manager of environment of input streaming module
   */
  private def createModuleEnvironmentManager() = {
    val taggedOutputs = inputInstance.outputs
      .map(ConnectionRepository.getStreamService.get)
      .filter(_.tags != null)

    new InputEnvironmentManager(taggedOutputs)
  }

  /**
   * Creates an eviction policy that defines a way of eviction of duplicate envelope
   * @return Eviction policy of duplicate envelopes
   */
  private def createEvictionPolicy() = {
    inputInstance.evictionPolicy match {
      case "fix-time" => new FixTimeEvictionPolicy(inputInstance)
      case "expanded-time" => new ExpandedTimeEvictionPolicy(inputInstance)
      case _ => new FixTimeEvictionPolicy(inputInstance)
    }
  }
}