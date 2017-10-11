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

import com.bwsw.sj.common.engine.core.entities.{EnvelopeInterface, InputEnvelope}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics

/**
  * Handles data for [[InputStreamingPerformanceMetricsReporter]] in separate thread
  *
  * @author Pavel Tomskikh
  */
class InputStreamingPerformanceMetrics(performanceMetrics: InputStreamingPerformanceMetricsReporter, threadName: String)
  extends PerformanceMetrics(performanceMetrics, threadName) {

  /**
    * Invokes when a new envelope from the input stream is received
    */
  override def addEnvelopeToInputStream(envelope: EnvelopeInterface): Unit = {
    val inputEnvelope = envelope.asInstanceOf[InputEnvelope[AnyRef]]
    addEnvelopeToInputStream(performanceMetrics.inputStreamName, List(inputEnvelope.data))
  }
}
