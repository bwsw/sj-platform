/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.engine.core.batch

import java.util.Calendar

import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.common.engine.core.entities._
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Class represents a set of metrics that characterize performance of a batch streaming module
  *
  * @param manager allows to manage an environment of batch streaming task
  * @author Kseniya Mikhaleva
  */

class BatchStreamingPerformanceMetrics(manager: CommonTaskManager)
  extends PerformanceMetrics(manager) {

  currentThread.setName(s"batch-task-${manager.taskName}-performance-metrics")
  private var totalIdleTime: Long = 0L
  private val batchInstance: BatchInstance = manager.instance.asInstanceOf[BatchInstance]
  private val inputStreamNames: Array[String] = manager.inputs.map(_._1.name).toArray
  private val outputStreamNames: Array[String] = batchInstance.outputs

  override protected var inputEnvelopesPerStream: mutable.Map[String, ListBuffer[List[Long]]] = createStorageForInputEnvelopes(inputStreamNames)
  override protected var outputEnvelopesPerStream: mutable.Map[String, mutable.Map[String, ListBuffer[Long]]] = createStorageForOutputEnvelopes(outputStreamNames)

  private var batchesPerStream: mutable.Map[String, ListBuffer[Int]] = createStorageForBatches()
  private var windowsPerStream: mutable.Map[String, Int] = createStorageForWindows()

  /**
    * Increases time when there are no messages (envelopes)
    *
    * @param idle How long waiting for a new envelope was
    */
  def increaseTotalIdleTime(idle: Long): Unit = {
    mutex.lock()
    totalIdleTime += idle
    mutex.unlock()
  }

  /**
    * Constructs a report of performance metrics of task work (one module could have multiple tasks)
    */
  override def getReport(): String = {
    logger.info(s"Start preparing a report of performance for task: ${manager.taskName} of batch module.")
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
    val numberOfBatchesPerStream = batchesPerStream.map(x => (x._1, x._2.size))
    val batchesElementsTotalNumber = batchesPerStream.map(x => x._2.sum).sum
    val batchesTotalNumber = numberOfBatchesPerStream.values.sum

    report.pmDatetime = Calendar.getInstance().getTime
    report.totalIdleTime = totalIdleTime
    report.totalInputEnvelopes = inputEnvelopesTotalNumber
    report.inputEnvelopesPerStream = numberOfInputEnvelopesPerStream.toMap
    report.totalInputElements = inputElementsTotalNumber
    report.inputElementsPerStream = numberOfInputElementsPerStream.toMap
    report.totalInputBytes = bytesOfInputEnvelopesPerStream.values.sum
    report.inputBytesPerStream = bytesOfInputEnvelopesPerStream.toMap
    report.averageSizeInputEnvelope = if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0
    report.maxSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0
    report.minSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0
    report.averageSizeInputElement = if (inputElementsTotalNumber != 0) bytesOfInputEnvelopesPerStream.values.sum / inputElementsTotalNumber else 0
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
    report.window = batchInstance.window
    report.batchesPerStream = numberOfBatchesPerStream.toMap
    report.averageSizeBatchPerStream = batchesPerStream.map(x => (x._1, average(x._2))).toMap
    report.totalBatches = batchesTotalNumber
    report.averageSizeBatch = if (batchesTotalNumber != 0) batchesElementsTotalNumber / batchesTotalNumber else 0
    report.maxSizeBatch = if (batchesTotalNumber != 0) batchesPerStream.map(x => if (x._2.nonEmpty) x._2.max else 0).max else 0
    report.minSizeBatch = if (batchesTotalNumber != 0) batchesPerStream.map(x => if (x._2.nonEmpty) x._2.max else 0).min else 0
    report.windowsPerStream = windowsPerStream.toMap
    report.uptime = (System.currentTimeMillis() - startTime) / 1000

    clear()

    mutex.unlock()
    reportSerializer.serialize(report)
  }

  override def clear(): Unit = {
    logger.debug(s"Reset variables for performance report for next reporting.")
    inputEnvelopesPerStream = mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Long]]())): _*)
    outputEnvelopesPerStream = mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Long]]())): _*)
    batchesPerStream = createStorageForBatches()
    windowsPerStream = createStorageForWindows()
    totalIdleTime = 0L
  }

  def addBatch(batch: Batch): ListBuffer[Int] = {
    batchesPerStream(batch.stream) += batch.envelopes.size
  }

  def addWindow(window: Window): Unit = {
    windowsPerStream(window.stream) += 1
  }

  private def createStorageForBatches(): mutable.Map[String, ListBuffer[Int]] = {
    mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[Int]())): _*)
  }

  private def createStorageForWindows(): mutable.Map[String, Int] = {
    mutable.Map(inputStreamNames.map(x => (x, 0)): _*)
  }

  private def average(elements: ListBuffer[Int]): Int = {
    val totalBatches = elements.size
    val sum = elements.sum
    if (totalBatches != 0) sum / totalBatches else 0
  }

}
