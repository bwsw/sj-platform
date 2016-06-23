package com.bwsw.sj.common.module

/**
 * Class represents a set of metrics that characterize performance of module
 * Created: 07/06/2016
 * @author Kseniya Mikhaleva
 */

import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock
import com.bwsw.common.JsonSerializer
import org.slf4j.LoggerFactory
import scala.collection._

abstract class PerformanceMetrics(taskId: String, host: String, inputStreamNames: Array[String], outputStreamNames: Array[String]) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val mutex: ReentrantLock
  protected val serializer = new JsonSerializer()
  protected val startTime = System.currentTimeMillis()
  protected var inputEnvelopesPerStream = mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
  protected var outputEnvelopesPerStream = mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)

  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  def getReport: String

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
        logger.error(s"Output stream with name: $name doesn't contain txn: $envelopeID\n")
        throw new Exception(s"Output stream with name: $name doesn't contain txn: $envelopeID")
      }
    } else {
      logger.error(s"Output stream with name: $name doesn't exist\n")
      throw new Exception(s"Output stream with name: $name doesn't exist")
    }
    mutex.unlock()
  }
}

class OutputStreamingPerformanceMetrics(taskId: String, host: String, inputStreamName: String, outputStreamName: String)
  extends PerformanceMetrics(taskId, host, Array(inputStreamName), Array(outputStreamName)) {

  val mutex: ReentrantLock = new ReentrantLock(true)

  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  def getReport = {
    logger.info(s"Start preparing a report of performance for task: $taskId of output module\n")
    mutex.lock()
    val bytesOfInputEnvelopes = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum)).head._2
    val bytesOfOutputEnvelopes = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.sum).sum)).head._2
    val inputEnvelopesTotalNumber = inputEnvelopesPerStream.map(x => (x._1, x._2.size)).head._2
    val inputElementsTotalNumber = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum)).head._2
    val outputEnvelopesTotalNumber = outputEnvelopesPerStream.map(x => (x._1, x._2.size)).head._2
    val outputElementsTotalNumber = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.size).sum)).head._2
    val inputEnvelopesSize = inputEnvelopesPerStream.flatMap(x => x._2.map(_.size))
    val outputEnvelopesSize = outputEnvelopesPerStream.flatMap(x => x._2.map(_._2.size))

    val performanceReport =
      s"""{
         |"datetime" : "${LocalDateTime.now()}",
         |"task-id" : "$taskId",
         |"host" : "$host",
         |"input-stream-name" : "$inputStreamName",
         |"total-input-envelopes" : $inputEnvelopesTotalNumber,
         |"total-input-elements" : $inputElementsTotalNumber,
         |"total-input-bytes" : $bytesOfInputEnvelopes,
         |"average-size-input-envelope" : ${if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0},
         |"max-size-input-envelope" : ${if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0},
         |"min-size-input-envelope" : ${if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0},
         |"average-size-input-element" : ${if (inputElementsTotalNumber != 0) bytesOfInputEnvelopes / inputElementsTotalNumber else 0},
         |"output-stream-name" : "$outputStreamName",
         |"total-output-envelopes" : $outputEnvelopesTotalNumber,
         |"total-output-elements" : $outputElementsTotalNumber,
         |"total-output-bytes" : $bytesOfOutputEnvelopes,
         |"average-size-output-envelope" : ${if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0},
         |"max-size-output-envelope" : ${if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0},
         |"min-size-output-envelope" : ${if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0},
         |"average-size-output-element" : ${if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopes / outputElementsTotalNumber else 0},
         |"uptime" : ${(System.currentTimeMillis() - startTime) / 1000}
      }
    """.stripMargin

    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream = mutable.Map(inputStreamName -> mutable.ListBuffer[List[Int]]())
    outputEnvelopesPerStream = mutable.Map(outputStreamName -> mutable.Map[String, mutable.ListBuffer[Int]]())

    mutex.unlock()

    performanceReport
  }
}

class RegularStreamingPerformanceMetrics(taskId: String, host: String, inputStreamNames: Array[String], outputStreamNames: Array[String])
  extends PerformanceMetrics(taskId, host, inputStreamNames, outputStreamNames) {

  val mutex: ReentrantLock = new ReentrantLock(true)
  private var totalIdleTime = 0L
  private var numberOfStateVariables = 0

  /**
   * Increases time when there are no messages (envelopes)
   * @param idle How long waiting a new envelope was
   */
  def increaseTotalIdleTime(idle: Long) = {
    mutex.lock()
    logger.debug(s"Increase total idle time on $idle ms\n")
    totalIdleTime += idle
    mutex.unlock()
  }

  /**
   * Sets an amount of how many state variables are
   * @param amount Number of state variables
   */
  def setNumberOfStateVariables(amount: Int) = {
    mutex.lock()
    logger.debug(s"Set number of state variables to $amount\n")
    numberOfStateVariables = amount
    mutex.unlock()
  }


  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  def getReport = {
    logger.info(s"Start preparing a report of performance for task: $taskId of regular module\n")
    mutex.lock()
    val numberOfInputEnvelopesPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfInputElementsPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum))
    val numberOfOutputElementsPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.size).sum))
    val bytesOfInputEnvelopesPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum))
    val bytesOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.sum).sum))
    val inputEnvelopesTotalNumber = numberOfInputEnvelopesPerStream.values.sum
    val inputElementsTotalNumber = numberOfInputElementsPerStream.values.sum
    val outputEnvelopesTotalNumber = numberOfOutputEnvelopesPerStream.values.sum
    val outputElementsTotalNumber = numberOfOutputElementsPerStream.values.sum
    val inputEnvelopesSize = inputEnvelopesPerStream.flatMap(x => x._2.map(_.size))
    val outputEnvelopesSize = outputEnvelopesPerStream.flatMap(x => x._2.map(_._2.size))

    val performanceReport =
      s"""{
         |"datetime" : "${LocalDateTime.now()}",
         |"task-id" : "$taskId",
         |"host" : "$host",
         |"total-idle-time" : $totalIdleTime,
         |"total-input-envelopes" : $inputEnvelopesTotalNumber,
         |"input-envelopes-per-stream" : ${serializer.serialize(numberOfInputEnvelopesPerStream)},
         |"total-input-elements" : $inputElementsTotalNumber,
         |"input-elements-per-stream" : ${serializer.serialize(numberOfInputElementsPerStream)},
         |"total-input-bytes" : ${bytesOfInputEnvelopesPerStream.values.sum},
         |"input-bytes-per-stream" : ${serializer.serialize(bytesOfInputEnvelopesPerStream)},
         |"average-size-input-envelope" : ${if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0},
         |"max-size-input-envelope" : ${if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0},
         |"min-size-input-envelope" : ${if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0},
         |"average-size-input-element" : ${if (inputElementsTotalNumber != 0) bytesOfInputEnvelopesPerStream.values.sum / inputElementsTotalNumber else 0},
         |"total-output-envelopes" : $outputEnvelopesTotalNumber,
         |"output-envelopes-per-stream" : ${serializer.serialize(numberOfOutputEnvelopesPerStream)},
         |"total-output-elements" : $outputElementsTotalNumber,
         |"output-elements-per-stream" : ${serializer.serialize(numberOfOutputElementsPerStream)},
         |"total-output-bytes" : ${bytesOfOutputEnvelopesPerStream.values.sum},
         |"output-bytes-per-stream" : ${serializer.serialize(bytesOfOutputEnvelopesPerStream)},
         |"average-size-output-envelope" : ${if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0},
         |"max-size-output-envelope" : ${if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0},
         |"min-size-output-envelope" : ${if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0},
         |"average-size-output-element" : ${if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopesPerStream.values.sum / outputElementsTotalNumber else 0},
         |"state-variables-number" : $numberOfStateVariables,
         |"uptime" : ${(System.currentTimeMillis() - startTime) / 1000}
      }
    """.stripMargin

    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream = mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
    outputEnvelopesPerStream = mutable.Map(inputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)
    totalIdleTime = 0L
    numberOfStateVariables = 0

    mutex.unlock()

    performanceReport
  }
}
