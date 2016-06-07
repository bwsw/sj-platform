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
//todo documentation and logging
class PerformanceMetrics(taskId: String, host: String, streamNames: Array[String]) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val mutex = new ReentrantLock(true)
  private val serializer = new JsonSerializer()
  private var inputEnvelopesPerStream = mutable.Map(streamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
  private var outputEnvelopesPerStream = mutable.Map(streamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
  private var totalIdleTime = 0L
  private var numberOfStateVariables = 0
  private val startTime = System.currentTimeMillis()

  def increaseTotalIdleTime(idle: Long) = {
    mutex.lock()
    totalIdleTime += idle
    mutex.unlock()
  }

  def setNumberOfStateVariables(amount: Int) = {
    mutex.lock()
    numberOfStateVariables += amount
    mutex.unlock()
  }

  def addEnvelopeToInputStream(name: String, elements: List[Int]) = {
    mutex.lock()
    if (inputEnvelopesPerStream.contains(name)) {
      inputEnvelopesPerStream(name) += elements
    } else {
      logger.error(s"Steam with name: $name doesn't exist\n")
      throw new Exception(s"Steam with name: $name doesn't exist")
    }
    mutex.unlock()
  }

  def getReport = {
    logger.info(s"Start preparing report of performance for task: $taskId of regular module\n")
    mutex.lock()
    val numberOfInputEnvelopesPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfInputElementsPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum))
    val numberOfOutputElementsPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum))
    val bytesOfInputEnvelopesPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum))
    val bytesOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum))
    val inputEnvelopesTotalNumber = numberOfInputEnvelopesPerStream.values.sum
    val inputElementsTotalNumber = numberOfInputElementsPerStream.values.sum
    val outputEnvelopesTotalNumber = numberOfOutputEnvelopesPerStream.values.sum
    val outputElementsTotalNumber = numberOfOutputElementsPerStream.values.sum
    val inputEnvelopesSize = inputEnvelopesPerStream.flatMap(x => x._2.map(_.size))
    val outputEnvelopesSize = outputEnvelopesPerStream.flatMap(x => x._2.map(_.size))

    val performanceReport =
    s"""{
    |"datetime" : "${LocalDateTime.now()}"
    |"task-id" : "$taskId",
    |"host" : "$host",
    |"total-idle-time" : "$totalIdleTime",
    |"total-input-envelopes" : "${inputEnvelopesTotalNumber}",
    |"input-envelopes-per-stream" : ${serializer.serialize(numberOfInputEnvelopesPerStream)},
    |"total-input-elements" : "${inputElementsTotalNumber}",
    |"input-elements-per-stream" : ${serializer.serialize(numberOfInputElementsPerStream)},
    |"total-input-bytes" : "${bytesOfInputEnvelopesPerStream.values.sum}",
    |"input-bytes-per-stream" : ${serializer.serialize(bytesOfInputEnvelopesPerStream)},
    |"average-size-input-envelope" : "${if (inputElementsTotalNumber != 0)  inputElementsTotalNumber / inputEnvelopesTotalNumber else 0}",
    |"max-size-input-envelope" : "${if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0}",
    |"min-size-input-envelope" : "${if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0}",
    |"average-size-input-element" : "${if (inputElementsTotalNumber != 0) bytesOfInputEnvelopesPerStream.values.sum / inputElementsTotalNumber else 0}",
    |"total-output-envelopes" : "${outputEnvelopesTotalNumber}",
    |"output-envelopes-per-stream" : ${serializer.serialize(numberOfOutputEnvelopesPerStream)},
    |"total-output-elements" : "${outputElementsTotalNumber}",
    |"output-elements-per-stream" : ${serializer.serialize(numberOfOutputElementsPerStream)},
    |"total-output-bytes" : "${bytesOfOutputEnvelopesPerStream.values.sum}",
    |"output-bytes-per-stream" : ${serializer.serialize(bytesOfOutputEnvelopesPerStream)},
    |"average-size-output-envelope" : "${if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0}",
    |"max-size-output-envelope" : "${if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0}",
    |"min-size-output-envelope" : "${if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0}",
    |"average-size-output-element" : "${if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopesPerStream.values.sum / outputElementsTotalNumber else 0}",
    |"state-variables-number" : "$numberOfStateVariables",
    |"uptime" : "${(System.currentTimeMillis() - startTime) / 1000}",
      }
    """.stripMargin

    inputEnvelopesPerStream = mutable.Map(streamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
    outputEnvelopesPerStream = mutable.Map(streamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
    totalIdleTime = 0L
    numberOfStateVariables = 0

    mutex.unlock()

    performanceReport
  }
}
