package com.bwsw.sj.engine.input.task.reporting

import java.util.Calendar

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.input.task.InputTaskManager

import scala.collection.mutable

/**
 * Class represents a set of metrics that characterize performance of a input streaming module
 * Created: 14/07/2016
 * @author Kseniya Mikhaleva
 */

class InputStreamingPerformanceMetrics(manager: InputTaskManager)
  extends PerformanceMetrics(manager) {

  currentThread.setName(s"input-task-${manager.taskName}-performance-metrics")
  private val inputStreamName = manager.agentsHost + ":" + manager.entryPort
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
  override def getReport(): String = {
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

    report.pmDatetime = Calendar.getInstance().getTime
    report.entryPointPort = manager.entryPort
    report.totalInputEnvelopes = inputEnvelopesTotalNumber
    report.totalInputElements = inputElementsTotalNumber
    report.totalInputBytes = bytesOfInputEnvelopes
    report.averageSizeInputEnvelope = if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0
    report.maxSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0
    report.minSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0
    report.averageSizeInputElement = if (inputElementsTotalNumber != 0) bytesOfInputEnvelopes / inputElementsTotalNumber else 0
    report.totalOutputEnvelopes = outputEnvelopesTotalNumber
    report.totalOutputElements = outputElementsTotalNumber
    report.outputEnvelopesPerStream = numberOfOutputEnvelopesPerStream.toMap
    report.outputElementsPerStream = numberOfOutputElementsPerStream.toMap
    report.outputBytesPerStream = bytesOfOutputEnvelopesPerStream.toMap
    report.totalOutputBytes = bytesOfOutputEnvelopesPerStream.values.sum
    report.averageSizeOutputEnvelope = if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0
    report.maxSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0
    report.minSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0
    report.averageSizeOutputElement = if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopesPerStream.values.sum / outputElementsTotalNumber else 0
    report.uptime = (System.currentTimeMillis() - startTime) / 1000

    clear()

    mutex.unlock()

    reportSerializer.serialize(report)
  }

  override def clear() = {
    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream = mutable.Map(inputStreamName -> mutable.ListBuffer[List[Int]]())
    outputEnvelopesPerStream = mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)
  }
}
