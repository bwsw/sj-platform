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
package com.bwsw.sj.engine.core.reporting

import java.util.Date

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}

/**
  * Class represents a set of metrics that characterize performance of module.
  *
  * @author Kseniya Mikhaleva
  */
class PerformanceMetricsMetadata {
  @JsonProperty("pm-datetime") var pmDatetime: Date = _
  @JsonProperty("task-id") var taskId: String = _
  var host: String = _
  @JsonProperty("total-input-envelopes") var totalInputEnvelopes: Int = 0
  @JsonProperty("total-input-elements") var totalInputElements: Int = 0
  @JsonProperty("total-input-bytes") var totalInputBytes: Int = 0
  @JsonProperty("average-size-input-envelope") var averageSizeInputEnvelope: Int = 0
  @JsonProperty("max-size-input-envelope") var maxSizeInputEnvelope: Int = 0
  @JsonProperty("min-size-input-envelope") var minSizeInputEnvelope: Int = 0
  @JsonProperty("average-size-input-element") var averageSizeInputElement: Int = 0
  @JsonProperty("total-output-envelopes") var totalOutputEnvelopes: Int = 0
  @JsonProperty("total-output-elements") var totalOutputElements: Int = 0
  @JsonProperty("total-output-bytes") var totalOutputBytes: Int = 0
  @JsonProperty("average-size-output-envelope") var averageSizeOutputEnvelope: Int = 0
  @JsonProperty("max-size-output-envelope") var maxSizeOutputEnvelope: Int = 0
  @JsonProperty("min-size-output-envelope") var minSizeOutputEnvelope: Int = 0
  @JsonProperty("average-size-output-element") var averageSizeOutputElement: Int = 0
  var uptime: Long = 0

  @JsonInclude(Include.NON_DEFAULT) @JsonProperty("total-idle-time") var totalIdleTime: Long = -1
  @JsonProperty("input-envelopes-per-stream") var inputEnvelopesPerStream: Map[String, Any] = _
  @JsonProperty("input-elements-per-stream") var inputElementsPerStream: Map[String, Any] = _
  @JsonProperty("input-bytes-per-stream") var inputBytesPerStream: Map[String, Any] = _
  @JsonProperty("output-envelopes-per-stream") var outputEnvelopesPerStream: Map[String, Any] = _
  @JsonProperty("output-elements-per-stream") var outputElementsPerStream: Map[String, Any] = _
  @JsonProperty("output-bytes-per-stream") var outputBytesPerStream: Map[String, Any] = _

  @JsonProperty("input-stream-name") var inputStreamName: String = _
  @JsonProperty("output-stream-name") var outputStreamName: String = _

  @JsonInclude(Include.NON_DEFAULT) @JsonProperty("entry-point-port") var entryPointPort: Int = -1

  @JsonInclude(Include.NON_DEFAULT) var window: Int = 0
  @JsonProperty("batches-per-stream") var batchesPerStream: Map[String, Int] = _
  @JsonProperty("average-size-batch-per-stream") var averageSizeBatchPerStream: Map[String, Int] = _
  @JsonInclude(Include.NON_DEFAULT) @JsonProperty("total-batches") var totalBatches: Int = -1
  @JsonInclude(Include.NON_DEFAULT) @JsonProperty("average-size-batch") var averageSizeBatch: Int = -1
  @JsonInclude(Include.NON_DEFAULT) @JsonProperty("max-size-batch") var maxSizeBatch: Int = -1
  @JsonInclude(Include.NON_DEFAULT) @JsonProperty("min-size-batch") var minSizeBatch: Int = -1
  @JsonProperty("windows-per-stream") var windowsPerStream: Map[String, Int] = _
}
