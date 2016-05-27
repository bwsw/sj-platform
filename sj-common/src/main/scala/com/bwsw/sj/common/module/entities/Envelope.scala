package com.bwsw.sj.common.module.entities

import com.bwsw.sj.common.module.output.model.OutputEnvelope
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input kafka stream or t-stream.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "streamType")
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamEnvelope], name = "stream.t-stream"),
  new Type(value = classOf[KafkaEnvelope], name = "stream.kafka"),
  new Type(value = classOf[OutputEnvelope], name = "elasticsearch-output"),
  new Type(value = classOf[OutputEnvelope], name = "jdbc-output")
))
class Envelope() {
  var streamType: String = null
  var stream: String = null
  var partition: Int = 0
  var tags: Array[String] = null
}
