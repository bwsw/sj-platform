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

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.utils.EngineLiterals

/**
  * @author Pavel Tomskikh
  */
class InputStreamingPerformanceMetricsThread(performanceMetrics: InputStreamingPerformanceMetrics, threadName: String)
  extends Thread(threadName) {

  private val envelopesQueue =
    new ArrayBlockingQueue[Either[InputEnvelope[AnyRef], (String, String, Long)]](EngineLiterals.queueSize)

  def addEnvelopeToInputStream(inputEnvelope: InputEnvelope[AnyRef]): Unit =
    envelopesQueue.put(Left(inputEnvelope))

  def addElementToOutputEnvelope(name: String, envelopeID: String, elementSize: Long): Unit =
    envelopesQueue.put(Right((name, envelopeID, elementSize)))

  override def run(): Unit = {
    while (true) {
      envelopesQueue.poll(EngineLiterals.eventWaitTimeout, TimeUnit.MILLISECONDS) match {
        case Left(inputEnvelope) =>
          performanceMetrics.addEnvelopeToInputStream(inputEnvelope)
        case Right((name, envelopeID, elementSize)) =>
          performanceMetrics.addElementToOutputEnvelope(name, envelopeID, elementSize)
        case _ =>
      }
    }
  }
}
