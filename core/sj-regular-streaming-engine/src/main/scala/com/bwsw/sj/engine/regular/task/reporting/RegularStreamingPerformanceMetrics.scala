package com.bwsw.sj.engine.regular.task.reporting

import java.util.Calendar

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.regular.task.RegularTaskManager

import scala.collection.mutable

/**
 * Class represents a set of metrics that characterize performance of a regular streaming module
 * Created: 07/06/2016
 * @author Kseniya Mikhaleva
 */

class RegularStreamingPerformanceMetrics(manager: RegularTaskManager)
  extends PerformanceMetrics(manager) {

  currentThread.setName(s"regular-task-${manager.taskName}-performance-metrics")
  private var totalIdleTime = 0L
  private var numberOfStateVariables = 0
  private val inputStreamNames =  manager.inputs.map(_._1.name).toArray
  private val outputStreamNames = manager.getInstance.outputs

  override protected var inputEnvelopesPerStream = createStorageForInputEnvelopes(inputStreamNames)
  override protected var outputEnvelopesPerStream = createStorageForOutputEnvelopes(outputStreamNames)
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
  override def getReport(): String = {
    logger.info(s"Start preparing a report of performance for task: ${manager.taskName} of regular module\n")
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

    performanceReport.pmDatetime = Calendar.getInstance().getTime
    performanceReport.totalIdleTime = totalIdleTime
    performanceReport.totalInputEnvelopes = inputEnvelopesTotalNumber
    performanceReport.inputEnvelopesPerStream = numberOfInputEnvelopesPerStream.toMap
    performanceReport.totalInputElements = inputElementsTotalNumber
    performanceReport.inputElementsPerStream = numberOfInputElementsPerStream.toMap
    performanceReport.totalInputBytes = bytesOfInputEnvelopesPerStream.values.sum
    performanceReport.inputBytesPerStream = bytesOfInputEnvelopesPerStream.toMap
    performanceReport.averageSizeInputEnvelope = if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0
    performanceReport.maxSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0
    performanceReport.minSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0
    performanceReport.averageSizeInputElement = if (inputElementsTotalNumber != 0) bytesOfInputEnvelopesPerStream.values.sum / inputElementsTotalNumber else 0
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
    performanceReport.stateVariablesNumber = numberOfStateVariables
    performanceReport.uptime = (System.currentTimeMillis() - startTime) / 1000

    clear()

    mutex.unlock()

    serializer.serialize(performanceReport)
  }

  override def clear() = {
    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream = mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
    outputEnvelopesPerStream = mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)
    totalIdleTime = 0L
    numberOfStateVariables = 0
  }
}
