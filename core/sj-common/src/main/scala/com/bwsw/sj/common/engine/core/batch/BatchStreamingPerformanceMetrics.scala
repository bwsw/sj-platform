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

import com.bwsw.sj.common.engine.core.entities.{Batch, Window}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics

/**
  * Handles data for [[BatchStreamingPerformanceMetricsReporter]] in separate thread
  *
  * @author Pavel Tomskikh
  */
class BatchStreamingPerformanceMetrics(performanceMetrics: BatchStreamingPerformanceMetricsReporter, threadName: String)
  extends PerformanceMetrics(performanceMetrics, threadName) {

  import com.bwsw.sj.common.engine.core.batch.BatchStreamingPerformanceMetrics._

  /**
    * Increases time when there are no messages (envelopes)
    *
    * @param idle How long waiting for a new envelope was
    */
  def increaseTotalIdleTime(idle: Long): Unit =
    envelopesQueue.put(IdleTimeIncreasing(idle))

  def addBatch(batch: Batch): Unit =
    envelopesQueue.put(BatchMessage(batch))

  def addWindow(window: Window): Unit =
    envelopesQueue.put(WindowMessage(window))

  override def handleCustomMessage(message: PerformanceMetrics.Message): Unit = message match {
    case IdleTimeIncreasing(idle) => performanceMetrics.increaseTotalIdleTime(idle)
    case WindowMessage(window) => performanceMetrics.addWindow(window)
    case BatchMessage(batch) => performanceMetrics.addBatch(batch)
  }
}

object BatchStreamingPerformanceMetrics {

  import PerformanceMetrics.Message

  case class IdleTimeIncreasing(idle: Long) extends Message

  case class WindowMessage(window: Window) extends Message

  case class BatchMessage(batch: Batch) extends Message

}
