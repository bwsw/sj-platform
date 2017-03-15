package com.bwsw.sj.module.output.pm.data

import java.util.Date

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.engine.core.entities.OutputEnvelope
import com.fasterxml.jackson.annotation.JsonProperty

/**
  *
  *
  * @author Kseniya Mikhaleva
  */
class PerformanceMetrics extends OutputEnvelope {
  val jsonSerializer = new JsonSerializer()
  @JsonProperty("pm-datetime") var pmDatetime: Date = null
  @JsonProperty("task-id") var taskId: String= null
  var host: String = null
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

  @JsonProperty("total-idle-time") var totalIdleTime: Long = 0
  @JsonProperty("input-envelopes-per-stream") var inputEnvelopesPerStream: Map[String, Any] = Map()
  @JsonProperty("input-elements-per-stream") var inputElementsPerStream: Map[String, Any] = Map()
  @JsonProperty("input-bytes-per-stream") var inputBytesPerStream: Map[String, Any] = Map()
  @JsonProperty("output-envelopes-per-stream") var outputEnvelopesPerStream: Map[String, Any] = Map()
  @JsonProperty("output-elements-per-stream") var outputElementsPerStream: Map[String, Any] = Map()
  @JsonProperty("output-bytes-per-stream") var outputBytesPerStream: Map[String, Any] = Map()
  @JsonProperty("state-variables-number") var stateVariablesNumber: Int = 0

  @JsonProperty("input-stream-name") var inputStreamName: String = null
  @JsonProperty("output-stream-name") var outputStreamName:  String = null

  def getMapFields: Map[String, AnyRef] = {
    Map(
      "pm-datetime" -> pmDatetime,
      "task-id" -> taskId,
      "total-input-envelopes" -> totalInputEnvelopes,
      "total-input-elements" -> totalInputElements,
      "total-input-bytes" -> totalInputBytes,
      "average-size-input-envelope" -> averageSizeInputEnvelope,
      "max-size-input-envelope" -> maxSizeInputEnvelope,
      "min-size-input-envelope" -> minSizeInputEnvelope,
      "average-size-input-element" -> averageSizeInputElement,
      "total-output-envelopes" -> totalOutputEnvelopes,
      "total-output-bytes" -> totalOutputBytes,
      "average-size-output-envelope" -> averageSizeOutputEnvelope,
      "max-size-output-envelope" -> maxSizeOutputEnvelope,
      "min-size-output-envelope" -> minSizeOutputEnvelope,
      "average-size-output-element" -> averageSizeOutputElement,
      "total-idle-time" -> totalIdleTime,
      "input-envelopes-per-stream" -> jsonSerializer.serialize(inputEnvelopesPerStream),
      "input-elements-per-stream" -> jsonSerializer.serialize(inputElementsPerStream),
      "input-bytes-per-stream" -> jsonSerializer.serialize(inputBytesPerStream),
      "output-envelopes-per-stream" -> jsonSerializer.serialize(outputEnvelopesPerStream),
      "output-elements-per-stream" -> jsonSerializer.serialize(outputElementsPerStream),
      "output-bytes-per-stream" -> jsonSerializer.serialize(outputBytesPerStream),
      "state-variables-number" -> stateVariablesNumber,
      "input-stream-name" -> inputStreamName,
      "output-stream-name" -> outputStreamName
    )
  }
}
