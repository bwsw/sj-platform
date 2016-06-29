package com.bwsw.sj.common.module.reporting

import java.util.Calendar
import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable

class OutputStreamingPerformanceMetrics(taskId: String, host: String, inputStreamName: String, outputStreamName: String)
  extends PerformanceMetrics(taskId, host, Array(inputStreamName), Array(outputStreamName)) {

  val mutex: ReentrantLock = new ReentrantLock(true)
  private val performanceReport = new PerformanceMetricsMetadata()

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

    performanceReport.pmDatetime = Calendar.getInstance().getTime
    performanceReport.taskId = taskId
    performanceReport.host = host
    performanceReport.inputStreamName = inputStreamName
    performanceReport.totalInputEnvelopes = inputEnvelopesTotalNumber
    performanceReport.totalInputElements = inputElementsTotalNumber
    performanceReport.totalInputBytes = bytesOfInputEnvelopes
    performanceReport.averageSizeInputEnvelope = if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0
    performanceReport.maxSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0
    performanceReport.minSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0
    performanceReport.averageSizeInputElement = if (inputElementsTotalNumber != 0) bytesOfInputEnvelopes / inputElementsTotalNumber else 0
    performanceReport.outputStreamName = outputStreamName
    performanceReport.totalOutputEnvelopes = outputEnvelopesTotalNumber
    performanceReport.totalOutputElements = outputElementsTotalNumber
    performanceReport.totalOutputBytes = bytesOfOutputEnvelopes
    performanceReport.averageSizeOutputEnvelope = if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0
    performanceReport.maxSizeOutputEnvelope =  if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0
    performanceReport.minSizeOutputEnvelope =  if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0
    performanceReport.averageSizeOutputElement =  if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopes / outputElementsTotalNumber else 0
    performanceReport.uptime = (System.currentTimeMillis () - startTime) / 1000

    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream = mutable.Map(inputStreamName -> mutable.ListBuffer[List[Int]]())
    outputEnvelopesPerStream = mutable.Map(outputStreamName -> mutable.Map[String, mutable.ListBuffer[Int]]())

    mutex.unlock()

    serializer.serialize(performanceReport)
  }

}