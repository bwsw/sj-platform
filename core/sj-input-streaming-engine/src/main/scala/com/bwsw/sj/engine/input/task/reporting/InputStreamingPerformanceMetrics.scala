package com.bwsw.sj.engine.input.task.reporting

import java.util.Calendar

import com.bwsw.sj.engine.core.reporting.{PerformanceMetrics, PerformanceMetricsMetadata}
import com.bwsw.sj.engine.input.task.InputTaskManager

import scala.collection.mutable

/**
 * Class represents a set of metrics that characterize performance of a input streaming module
 * Created: 14/07/2016
 * @author Kseniya Mikhaleva
 */

class InputStreamingPerformanceMetrics(manager: InputTaskManager)
  extends PerformanceMetrics(manager) {

  private val performanceReport = new PerformanceMetricsMetadata()
  private val inputStreamName = manager.entryHost + ":" + manager.entryPort
  private val outputStreamNames = instance.outputs

  override protected var inputEnvelopesPerStream = createStorageForInputEnvelopes(Array(inputStreamName))
  override protected var outputEnvelopesPerStream = createStorageForOutputEnvelopes(outputStreamNames)

  /**
   * Invokes when a new envelope from the input stream is received
   * @param elementsSize Set of sizes of elements
   */
  def addEnvelopeToInputStream(elementsSize: List[Int]) = {
    super.addEnvelopeToInputStream(inputStreamName, elementsSize)
  }

  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  override def getReport: String = {
    logger.info(s"Start preparing a report of performance for task: $taskName of input module\n")
    mutex.lock()
    val bytesOfInputEnvelopes = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum)).head._2
    val inputEnvelopesTotalNumber = inputEnvelopesPerStream.map(x => (x._1, x._2.size)).head._2
    val inputElementsTotalNumber = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum)).head._2
    val inputEnvelopesSize = inputEnvelopesPerStream.flatMap(x => x._2.map(_.size))
    val numberOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfOutputElementsPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.size).sum))
    val bytesOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.sum).sum))
    val outputEnvelopesTotalNumber = numberOfOutputEnvelopesPerStream.values.sum
    val outputElementsTotalNumber = numberOfOutputElementsPerStream.values.sum
    val outputEnvelopesSize = outputEnvelopesPerStream.flatMap(x => x._2.map(_._2.size))

    performanceReport.pmDatetime = Calendar.getInstance().getTime
    performanceReport.taskId = taskName
    performanceReport.host = manager.agentsHost //todo check it
    performanceReport.entryPointHost = manager.entryHost //todo check it
    performanceReport.entryPointPort = manager.entryPort
    performanceReport.totalInputEnvelopes = inputEnvelopesTotalNumber
    performanceReport.totalInputElements = inputElementsTotalNumber
    performanceReport.totalInputBytes = bytesOfInputEnvelopes
    performanceReport.averageSizeInputEnvelope = if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0
    performanceReport.maxSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0
    performanceReport.minSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0
    performanceReport.averageSizeInputElement = if (inputElementsTotalNumber != 0) bytesOfInputEnvelopes / inputElementsTotalNumber else 0
    performanceReport.totalOutputEnvelopes = outputEnvelopesTotalNumber
    performanceReport.totalOutputElements = outputElementsTotalNumber
    performanceReport.outputEnvelopesPerStream = numberOfOutputEnvelopesPerStream.toMap
    performanceReport.outputElementsPerStream = numberOfOutputElementsPerStream.toMap
    performanceReport.outputBytesPerStream = bytesOfOutputEnvelopesPerStream.toMap
    performanceReport.totalOutputBytes = bytesOfOutputEnvelopesPerStream.values.sum
    performanceReport.averageSizeOutputEnvelope = if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0
    performanceReport.maxSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0
    performanceReport.minSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0
    performanceReport.averageSizeOutputElement = if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopesPerStream.values.sum / outputElementsTotalNumber else 0
    performanceReport.uptime = (System.currentTimeMillis() - startTime) / 1000

    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream = mutable.Map(inputStreamName -> mutable.ListBuffer[List[Int]]())
    outputEnvelopesPerStream = mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)

    mutex.unlock()

    serializer.serialize(performanceReport)
  }
}
