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
package com.bwsw.sj.engine.regular.task.reporting

import java.util.Calendar

import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Class represents a set of metrics that characterize performance of a regular streaming module
  *
  * @param manager allows to manage an environment of regular streaming task
  * @author Kseniya Mikhaleva
  */

class RegularStreamingPerformanceMetrics(manager: CommonTaskManager)
  extends PerformanceMetrics(manager) {

  currentThread.setName(s"regular-task-${manager.taskName}-performance-metrics")
  private var totalIdleTime: Long = 0L
  private val inputStreamNames: Array[String] = manager.inputs.map(_._1.name).toArray
  private val outputStreamNames: Array[String] = manager.instance.outputs

  override protected var inputEnvelopesPerStream: mutable.Map[String, ListBuffer[List[Int]]] = createStorageForInputEnvelopes(inputStreamNames)
  override protected var outputEnvelopesPerStream: mutable.Map[String, mutable.Map[String, ListBuffer[Int]]] = createStorageForOutputEnvelopes(outputStreamNames)

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
    logger.info(s"Start preparing a report of performance for task: ${manager.taskName} of regular module.")
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
    report.uptime = (System.currentTimeMillis() - startTime) / 1000

    clear()

    mutex.unlock()

    reportSerializer.serialize(report)
  }

  override def clear(): Unit = {
    logger.debug(s"Reset variables for performance report for next reporting.")
    inputEnvelopesPerStream = mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[List[Int]]())): _*)
    outputEnvelopesPerStream = mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Int]]())): _*)
    totalIdleTime = 0L
  }
}
