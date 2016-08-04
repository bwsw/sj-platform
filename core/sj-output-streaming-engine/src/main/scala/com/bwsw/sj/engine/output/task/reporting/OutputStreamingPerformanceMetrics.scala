package com.bwsw.sj.engine.output.task.reporting

import java.util.Calendar

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.output.task.OutputTaskManager

import scala.collection.mutable

/**
 * Class represents a set of metrics that characterize performance of a output streaming module
 * Created: 07/06/2016
 * @author Kseniya Mikhaleva
 */

class OutputStreamingPerformanceMetrics(manager: OutputTaskManager)
  extends PerformanceMetrics(manager) {

  currentThread.setName(s"output-task-${manager.taskName}-performance-metrics")
  private val inputStreamNames = instance.inputs
  private val outputStreamNames = instance.outputs

  override protected var inputEnvelopesPerStream = createStorageForInputEnvelopes(inputStreamNames)
  override protected var outputEnvelopesPerStream = createStorageForOutputEnvelopes(outputStreamNames)

  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  override def getReport() = {
    logger.info(s"Start preparing a report of performance for task: $taskName of output module\n")
    mutex.lock()
    val bytesOfInputEnvelopes = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum)).head._2
    val bytesOfOutputEnvelopes = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.sum).sum)).head._2
    val inputEnvelopesTotalNumber = inputEnvelopesPerStream.map(x => (x._1, x._2.size)).head._2
    val inputElementsTotalNumber = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum)).head._2
    val outputEnvelopesTotalNumber = outputEnvelopesPerStream.map(x => (x._1, x._2.size)).head._2
    val outputElementsTotalNumber = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.size).sum)).head._2
    val inputEnvelopesSize = inputEnvelopesPerStream.flatMap(x => x._2.map(_.size))
    val outputEnvelopesSize = outputEnvelopesPerStream.flatMap(x => x._2.map(_._2.size))

    performanceReport.pmDatetime = Calendar.getInstance().getTime
    performanceReport.inputStreamName = inputStreamNames.head
    performanceReport.totalInputEnvelopes = inputEnvelopesTotalNumber
    performanceReport.totalInputElements = inputElementsTotalNumber
    performanceReport.totalInputBytes = bytesOfInputEnvelopes
    performanceReport.averageSizeInputEnvelope = if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0
    performanceReport.maxSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0
    performanceReport.minSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0
    performanceReport.averageSizeInputElement = if (inputElementsTotalNumber != 0) bytesOfInputEnvelopes / inputElementsTotalNumber else 0
    performanceReport.outputStreamName = outputStreamNames.head
    performanceReport.totalOutputEnvelopes = outputEnvelopesTotalNumber
    performanceReport.totalOutputElements = outputElementsTotalNumber
    performanceReport.totalOutputBytes = bytesOfOutputEnvelopes
    performanceReport.averageSizeOutputEnvelope = if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0
    performanceReport.maxSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0
    performanceReport.minSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0
    performanceReport.averageSizeOutputElement = if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopes / outputElementsTotalNumber else 0
    performanceReport.uptime = (System.currentTimeMillis() - startTime) / 1000

    clear()

    mutex.unlock()

    serializer.serialize(performanceReport)
  }

  override def clear() = {
    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream = mutable.Map(inputStreamNames.head -> mutable.ListBuffer[List[Int]]())
    outputEnvelopesPerStream = mutable.Map(outputStreamNames.head -> mutable.Map[String, mutable.ListBuffer[Int]]())
  }
}