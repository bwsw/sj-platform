package com.bwsw.sj.engine.core.reporting

/**
 * Class represents a set of metrics that characterize performance of module
 *
 * @author Kseniya Mikhaleva
 */

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{Callable, TimeUnit}

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.tstreams.agents.producer.NewTransactionProducerPolicy
import org.slf4j.LoggerFactory

import scala.collection._
import scala.collection.mutable.ListBuffer

abstract class PerformanceMetrics(manager: TaskManager) extends Callable[Unit] {

  protected val currentThread = Thread.currentThread()
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val mutex: ReentrantLock = new ReentrantLock(true)
  protected val reportSerializer = new JsonSerializer()
  protected val startTime = System.currentTimeMillis()
  protected var inputEnvelopesPerStream: mutable.Map[String, ListBuffer[List[Int]]]
  protected var outputEnvelopesPerStream: mutable.Map[String, mutable.Map[String, ListBuffer[Int]]]
  protected val taskName = manager.taskName
  protected val instance = manager.instance
  private val reportingInterval = instance.performanceReportingInterval
  protected val report = new PerformanceMetricsMetadata()
  private val reportStreamName = instance.name + "_report"
  private val reportStream = getReportStream()
  private val reportProducer = createReportProducer()

  fillStaticPerformanceMetrics()

  /**
   * Creates a SjStream to keep the reports of module performance.
   * For each task there is specific partition (task number = partition number).
   *
   * @return SjStream used for keeping the reports of module performance
   */
  private def getReportStream(): TStreamSjStream = {
    logger.debug(s"Task name: $taskName. " +
      s"Get stream for performance metrics.")
    val tags = Array("report", "performance")
    val description = "store reports of performance metrics"
    val partitions = instance.parallelism

    manager.createTStreamOnCluster(reportStreamName, description, partitions)

    manager.getSjStream(
      reportStreamName,
      description,
      tags,
      partitions
    )
  }

  /**
   * Create t-stream producer for stream for reporting
   * @return Producer for reporting performance metrics
   */
  private def createReportProducer() = {
    logger.debug(s"Task: $taskName. Start creating a t-stream producer to record performance reports.")
    val reportProducer = manager.createProducer(reportStream)
    logger.debug(s"Task: $taskName. Creation of t-stream producer is finished.")

    reportProducer
  }

  private def fillStaticPerformanceMetrics() = {
    report.taskId = taskName
    report.host = manager.agentsHost
  }

  /**
   * It's in charge of cleaning a report of performance metrics for next time
   */
  protected def clear(): Unit

  def addEnvelopeToInputStream(envelope: Envelope): Unit = {
    envelope match {
      case tStreamEnvelope: TStreamEnvelope =>
        addEnvelopeToInputStream(tStreamEnvelope.stream, tStreamEnvelope.data.map(_.length))
      case kafkaEnvelope: KafkaEnvelope =>
        addEnvelopeToInputStream(kafkaEnvelope.stream, List(kafkaEnvelope.data.length))
      case wrongEnvelope =>
        logger.error(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined")
        throw new Exception(s"Incoming envelope with type: ${wrongEnvelope.getClass} is not defined")
    }
  }

  protected def addEnvelopeToInputStream(name: String, elementsSize: List[Int]) = {
    mutex.lock()
    logger.debug(s"Indicate that a new envelope is received from input stream: $name.")
    if (inputEnvelopesPerStream.contains(name)) {
      inputEnvelopesPerStream(name) += elementsSize
    } else {
      logger.error(s"Input stream with name: $name doesn't exist.")
      throw new Exception(s"Input stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
   * Invokes when a new element is sent to txn of some output stream
   * @param name Stream name
   * @param envelopeID Id of envelope of output stream
   * @param elementSize Size of appended element
   */
  def addElementToOutputEnvelope(name: String, envelopeID: String, elementSize: Int) = {
    mutex.lock()
    logger.debug(s"Indicate that a new element is sent to txn: $envelopeID of output stream: $name.")
    if (outputEnvelopesPerStream.contains(name)) {
      if (outputEnvelopesPerStream(name).contains(envelopeID)) {
        outputEnvelopesPerStream(name)(envelopeID) += elementSize
      } else {
        logger.debug(s"Output stream with name: $name doesn't contain txn: $envelopeID.")
        outputEnvelopesPerStream(name) += (envelopeID -> ListBuffer(elementSize))
      }
    } else {
      logger.error(s"Output stream with name: $name doesn't exist.")
      throw new Exception(s"Output stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
   * It is in charge of running of input module
   */
  override def call() = {
    logger.debug(s"Task: $taskName. Launch a new thread to report performance metrics .")
    val currentThread = Thread.currentThread()
    currentThread.setName(s"report-task-$taskName")

    while (true) {
      logger.info(s"Task: $taskName. Wait $reportingInterval ms to report performance metrics.")
      TimeUnit.MILLISECONDS.sleep(reportingInterval)
      val report = getReport()
      logger.info(s"Task: $taskName. Performance metrics: $report .")
      sendReport(report)
      logger.debug(s"Task: $taskName. Do checkpoint of producer for performance reporting.")
      reportProducer.checkpoint()
    }
  }

  /**
   * Constructs a report of performance metrics of task work
   * @return Constructed performance report
   */
  def getReport(): String

  private def sendReport(report: String) = {
    val reportSerializerForTxn = new ObjectSerializer()
    val taskNumber = taskName.replace(s"${manager.instanceName}-task", "").toInt
    logger.debug(s"Task: $taskName. Create a new txn for sending performance metrics.")
    val reportTxn = reportProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened, taskNumber)
    logger.debug(s"Task: $taskName. Send performance metrics.")
    reportTxn.send(reportSerializerForTxn.serialize(report))
  }

  protected def createStorageForInputEnvelopes(inputStreamNames: Array[String]) = {
    mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
  }

  protected def createStorageForOutputEnvelopes(outputStreamNames: Array[String]) = {
    mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)
  }
}