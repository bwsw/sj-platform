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
package com.bwsw.sj.engine.input.task.reporting

import java.util.Calendar

import com.bwsw.common.ObjectSizeFetcher
import com.bwsw.sj.common.engine.core.entities.{Envelope, InputEnvelope}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.input.task.InputTaskManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Class represents a set of metrics that characterize performance of an input streaming module
  *
  * @param manager allows to manage an environment of input streaming task
  * @author Kseniya Mikhaleva
  */

class InputStreamingPerformanceMetrics(manager: InputTaskManager)
  extends PerformanceMetrics(manager) {

  currentThread.setName(s"input-task-${manager.taskName}-performance-metrics")
  private val inputStreamName: String = manager.agentsHost + ":" + manager.entryPort
  private val outputStreamNames: Array[String] = instance.outputs

  override protected var inputEnvelopesPerStream: mutable.Map[String, ListBuffer[List[Long]]] = createStorageForInputEnvelopes(Array(inputStreamName))
  override protected var outputEnvelopesPerStream: mutable.Map[String, mutable.Map[String, ListBuffer[Long]]] = createStorageForOutputEnvelopes(outputStreamNames)

  /**
    * Invokes when a new envelope from the input stream is received
    */
  override def addEnvelopeToInputStream(envelope: Envelope): Unit = {
    val inputEnvelope = envelope.asInstanceOf[InputEnvelope[AnyRef]]
    super.addEnvelopeToInputStream(inputStreamName, List(ObjectSizeFetcher.getObjectSize(inputEnvelope.data)))
  }

  /**
    * Constructs a report of performance metrics of task work (one module could have multiple tasks)
    */
  override def getReport(): String = {
    logger.info(s"Start preparing a report of performance for task: $taskName of an input module.")
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

  override def clear(): Unit = {
    logger.debug(s"Reset variables of performance report for next reporting.")
    inputEnvelopesPerStream = mutable.Map(inputStreamName -> mutable.ListBuffer[List[Long]]())
    outputEnvelopesPerStream = mutable.Map(outputStreamNames.map(x => (x, mutable.Map[String, mutable.ListBuffer[Long]]())): _*)
  }
}
