package com.bwsw.sj.engine.input

import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.{ExecutorService, TimeUnit}

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.module.reporting.InputStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import com.bwsw.sj.engine.input.eviction_policy.{FixTimeEvictionPolicy, ExpandedTimeEvictionPolicy}
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerTransaction, ProducerPolicies}
import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCountUtil
import org.slf4j.LoggerFactory

/**
 * Provides methods are responsible for a basic execution logic of task of input module
 * Created: 10/07/2016
 *
 * @param manager Manager of environment of task of input module
 * @param inputInstanceMetadata Input instance is a metadata for running a task of input module
 * @author Kseniya Mikhaleva
 */
abstract class InputTaskEngine(manager: InputTaskManager, inputInstanceMetadata: InputInstance) {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] = manager.createOutputProducers
  protected val streams = producers.keySet
  protected var txnsByStreamPartitions = createTxnsStorage(streams)
  protected val checkpointGroup = new CheckpointGroup()
  protected val moduleEnvironmentManager = createModuleEnvironmentManager()
  protected val executor: InputStreamingExecutor = manager.getExecutor(moduleEnvironmentManager)
  protected val evictionPolicy = createEvictionPolicy()
  protected val isNotOnlyCustomCheckpoint: Boolean
  protected val performanceMetrics = new InputStreamingPerformanceMetrics(
    manager.taskName,
    manager.agentsHost,
    manager.entryHost,
    manager.entryPort,
    inputInstanceMetadata.outputs
  )

  addProducersToCheckpointGroup()

  /**
   * It is responsible for sending an answer to client about the fact that a new txn is opened
   * Usually it will be invoked after checkpoint
   * @param txn Transaction UUID
   */
  protected def txnOpen(txn: UUID) = {
    println("txnOpen: UUID = " + txn) //todo
  }

  /**
   * It is responsible for sending an answer to client about the fact that a new txn is closed
   * Usually it will be invoked after checkpoint
   * @param txn Transaction UUID
   */
  protected def txnClose(txn: UUID) = {
    println("txnClose: UUID = " + txn) //todo
  }

  /**
   * It is responsible for sending an answer to client about the fact that a new txn is canceled
   * Usually it will be invoked after checkpoint
   * @param txn Transaction UUID
   */
  protected def txnCancel(txn: UUID) = {
    println("txnCancel: UUID = " + txn) //todo
  }

  /**
   * It is responsible for sending an answer to client about the fact that an envelope is processed
   * @param envelope Input envelope
   * @param isNotDuplicateOrEmpty Flag points whether a processed envelope is duplicate or empty or not.
   *                              If it is true it means a processed envelope is duplicate or empty and false in other case
   */
  protected def envelopeProcessed(envelope: Option[InputEnvelope], isNotDuplicateOrEmpty: Boolean) = {
    if (isNotDuplicateOrEmpty) {
      //todo
      println("Envelope has been sent")
    } else if (envelope.isDefined) {
      println("Envelope is duplicate")
    } else {
      println("Envelope is empty")
    }
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
      performanceMetrics.addEnvelopeToInputStream(List(inputEnvelope.data.length))
      if (checkForDuplication(inputEnvelope.key, inputEnvelope.duplicateCheck, inputEnvelope.data)) {
        inputEnvelope.outputMetadata.foreach(x => {
          sendEnvelope(x._1, x._2, inputEnvelope.data)
        })
        return true
      }
    }
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
      txn = maybeTxn.get
      txn.send(data)
    } else {
      txn = producers(stream).newTransaction(ProducerPolicies.errorIfOpen, partition)
      txn.send(data)
      txnOpen(txn.getTxnUUID)
    }

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
      s"Try to check key: $key for duplication with a setting duplicateCheck = $duplicateCheck\n")
    if (duplicateCheck) {
      logger.info(s"Task name: ${manager.taskName}. " +
        s"Check key: $key for duplication\n")
      evictionPolicy.checkForDuplication(key, value)
    } else true
  }

  /**
   * It is in charge of running a basic execution logic of input task engine
   * @param executorService Executor service for running a basic execution logic of input task engine
   *                        in a separate thread
   * @param buffer Buffer for keeping incoming bytes
   */
  def runModule(executorService: ExecutorService, buffer: ByteBuf) = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run input task engine in a separate thread of execution service\n")
    executorService.execute(new Runnable {
      override def run(): Unit = try {
        launchPerformanceMetricsReporting(executorService)

        while (true) {
          val maybeInterval = executor.tokenize(buffer)
          if (maybeInterval.isDefined) {
            val (beginIndex, endIndex) = maybeInterval.get
            if (buffer.isReadable(endIndex)) {
              println("before reading: " + buffer.toString(Charset.forName("UTF-8")) + "_") //todo: only for testing
              val inputEnvelope: Option[InputEnvelope] = executor.parse(buffer, beginIndex, endIndex)
              clearBufferAfterParsing(buffer, endIndex)
              println("after reading: " + buffer.toString(Charset.forName("UTF-8")) + "_") //todo: only for testing
              val isNotDuplicateOrEmpty = processEnvelope(inputEnvelope)
              envelopeProcessed(inputEnvelope, isNotDuplicateOrEmpty)
              doCheckpoint(moduleEnvironmentManager.isCheckpointInitiated)
            } else {
              logger.error(s"Task name: ${manager.taskName}. " +
                s"Method tokenize() returned end index that an input stream is not defined at\n")
              throw new IndexOutOfBoundsException("Method tokenize() returned end index that an input stream is not defined at")
            }
          } else Thread.sleep(2000) //todo: only for testing
        }
      } finally {
        ReferenceCountUtil.release(buffer)
      }
    })
  }

  /**
   * Create t-stream producer for stream for reporting
   * @return Producer for reporting performance metrics
   */
  private def createReportProducer() = {
    logger.debug(s"Task: ${manager.taskName}. Start creating a t-stream producer to record performance reports\n")
    val reportStream = manager.getReportStream
    val reportProducer = manager.createProducer(reportStream)
    logger.debug(s"Task: ${manager.taskName}. Creation of t-stream producer is finished\n")

    reportProducer
  }

  /**
   * It is in charge of running of input module
   * @param executorService Executor service for running an execution logic for creating report of performance metrics
   *                        in a separate thread
   */
  private def launchPerformanceMetricsReporting(executorService: ExecutorService) = {
    val reportProducer = createReportProducer()
    logger.debug(s"Task: ${manager.taskName}. Launch a new thread to report performance metrics \n")
    executorService.execute(new Runnable() {
      val objectSerializer = new ObjectSerializer()
      val currentThread = Thread.currentThread()
      currentThread.setName(s"report-task-${manager.taskName}")

      def run() = {
        val taskNumber = manager.taskName.replace(s"${manager.instanceName}-task", "").toInt
        var report: String = null
        var reportTxn: BasicProducerTransaction[Array[Byte], Array[Byte]] = null
        while (true) {
          logger.info(s"Task: ${manager.taskName}. Wait ${inputInstanceMetadata.performanceReportingInterval} ms to report performance metrics\n")
          TimeUnit.MILLISECONDS.sleep(inputInstanceMetadata.performanceReportingInterval)
          report = performanceMetrics.getReport
          println(s"Performance metrics: $report \n")
          logger.info(s"Task: ${manager.taskName}. Performance metrics: $report \n")
          logger.debug(s"Task: ${manager.taskName}. Create a new txn for sending performance metrics\n")
          reportTxn = reportProducer.newTransaction(ProducerPolicies.errorIfOpen, taskNumber)
          logger.debug(s"Task: ${manager.taskName}. Send performance metrics\n")
          reportTxn.send(objectSerializer.serialize(report))
          logger.debug(s"Task: ${manager.taskName}. Do checkpoint of producer for performance reporting\n")
          reportProducer.checkpoint()
        }
      }
    })
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside input module (not on the schedule) or not.
   */
  protected def doCheckpoint(isCheckpointInitiated: Boolean): Unit

  /**
   *
   * @param buffer A buffer for keeping incoming bytes
   * @param endIndex Index that was last at reading
   */
  protected def clearBufferAfterParsing(buffer: ByteBuf, endIndex: Int) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Remove a message, which have just been parsed, from input buffer\n")
    buffer.readerIndex(endIndex)
    buffer.discardReadBytes()
  }

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
   * Creates a map that keeps current open txn for each partition of output stream
   * @param streams Set of names of output streams
   * @return Map where a key is output stream name, a value is a map
   *         in which key is a number of partition and value is a txn
   */
  protected def createTxnsStorage(streams: Set[String]) = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Create storage for keeping txns for each partition of output streams\n")
    streams.map(x => (x, Map[Int, BasicProducerTransaction[Array[Byte], Array[Byte]]]())).toMap
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
    val taggedOutputs = inputInstanceMetadata.outputs
      .map(ConnectionRepository.getStreamService.get)
      .filter(_.tags != null)

    new InputEnvironmentManager(taggedOutputs)
  }

  /**
   * Creates an eviction policy that defines a way of eviction of duplicate envelope
   * @return Eviction policy of duplicate envelopes
   */
  private def createEvictionPolicy() = {
    inputInstanceMetadata.evictionPolicy match {
      case "fix-time" => new FixTimeEvictionPolicy(manager)
      case "expanded-time" => new ExpandedTimeEvictionPolicy(manager)
    }
    //new ExpandedTimeEvictionPolicy(manager) //todo for testing
  }
}