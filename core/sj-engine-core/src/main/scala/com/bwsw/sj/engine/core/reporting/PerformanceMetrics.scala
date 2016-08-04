package com.bwsw.sj.engine.core.reporting

/**
 * Class represents a set of metrics that characterize performance of module
 * Created: 07/06/2016
 * @author Kseniya Mikhaleva
 */

import java.util.concurrent.{Callable, TimeUnit}
import java.util.concurrent.locks.ReentrantLock

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, ProducerTransaction}
import org.slf4j.LoggerFactory

import scala.collection._
import scala.collection.mutable.ListBuffer

abstract class PerformanceMetrics(manager: TaskManager) extends Callable[Unit] {

  protected val currentThread = Thread.currentThread()
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val mutex: ReentrantLock = new ReentrantLock(true)
  protected val serializer = new JsonSerializer()
  protected val startTime = System.currentTimeMillis()
  protected var inputEnvelopesPerStream: mutable.Map[String, ListBuffer[List[Int]]]
  protected var outputEnvelopesPerStream: mutable.Map[String, mutable.Map[String, ListBuffer[Int]]]
  protected val taskName = manager.taskName
  protected val reportingInterval = manager.getInstance.performanceReportingInterval
  protected val instance = manager.getInstance
  protected val performanceReport = new PerformanceMetricsMetadata()

  fillStaticPerformanceMetrics()

  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  def getReport(): String

  /**
   * It's in charge of cleaning a report of performance metrics for next time
   */
  protected def clear(): Unit

  /**
   * Invokes when a new envelope from some input stream is received
   * @param name Stream name
   * @param elementsSize Set of sizes of elements
   */
  def addEnvelopeToInputStream(name: String, elementsSize: List[Int]) = {
    mutex.lock()
    logger.debug(s"Indicate that a new envelope is received from input stream: $name\n")
    if (inputEnvelopesPerStream.contains(name)) {
      inputEnvelopesPerStream(name) += elementsSize
    } else {
      logger.error(s"Input stream with name: $name doesn't exist\n")
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
    logger.debug(s"Indicate that a new element is sent to txn: $envelopeID of output stream: $name\n")
    if (outputEnvelopesPerStream.contains(name)) {
      if (outputEnvelopesPerStream(name).contains(envelopeID)) {
        outputEnvelopesPerStream(name)(envelopeID) += elementSize
      } else {
        logger.debug(s"Output stream with name: $name doesn't contain txn: $envelopeID\n")
        outputEnvelopesPerStream(name) += (envelopeID -> ListBuffer(elementSize))
      }
    } else {
      logger.error(s"Output stream with name: $name doesn't exist\n")
      throw new Exception(s"Output stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  /**
   * It is in charge of running of input module
   */
  override def call() = {

    val reportProducer = createReportProducer()
    logger.debug(s"Task: $taskName. Launch a new thread to report performance metrics \n")
    val objectSerializer = new ObjectSerializer()
    val currentThread = Thread.currentThread()
    currentThread.setName(s"report-task-$taskName")

    val taskNumber = taskName.replace(s"${manager.instanceName}-task", "").toInt
    var report: String = null
    var reportTxn: ProducerTransaction[Array[Byte]] = null
    while (true) {
      logger.info(s"Task: $taskName. Wait $reportingInterval ms to report performance metrics\n")
      TimeUnit.MILLISECONDS.sleep(reportingInterval)
      report = getReport()
      println(s"Performance metrics: $report \n")
      logger.info(s"Task: $taskName. Performance metrics: $report \n")
      logger.debug(s"Task: $taskName. Create a new txn for sending performance metrics\n")
      reportTxn = reportProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened, taskNumber)
      logger.debug(s"Task: $taskName. Send performance metrics\n")
      reportTxn.send(objectSerializer.serialize(report))
      logger.debug(s"Task: $taskName. Do checkpoint of producer for performance reporting\n")
      reportProducer.checkpoint()
    }
  }

  /**
   * Create t-stream producer for stream for reporting
   * @return Producer for reporting performance metrics
   */
  private def createReportProducer() = {
    logger.debug(s"Task: $taskName. Start creating a t-stream producer to record performance reports\n")
    val reportStream = manager.reportStream
    val reportProducer = manager.createProducer(reportStream)
    logger.debug(s"Task: $taskName. Creation of t-stream producer is finished\n")

    reportProducer
  }


  protected def createStorageForInputEnvelopes(inputStreamNames: Array[String]) = {
    mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
  }

  protected def createStorageForOutputEnvelopes(outputStreamNames: Array[String]) = {
    mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)
  }

  private def fillStaticPerformanceMetrics() = {
    performanceReport.taskId = taskName
    performanceReport.host = manager.agentsHost
  }

}