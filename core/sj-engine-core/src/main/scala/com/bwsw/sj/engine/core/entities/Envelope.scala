package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input (kafka, t-stream, elasticsearch, jdbc)
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "streamType")
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamEnvelope], name = "stream.t-stream"),
  new Type(value = classOf[KafkaEnvelope], name = "stream.kafka"),
  new Type(value = classOf[EsEnvelope], name = "elasticsearch-output"),
  new Type(value = classOf[JdbcEnvelope], name = "jdbc-output")
))
class Envelope() {
  protected var streamType: String = null
  var stream: String = null
  var partition: Int = 0
  var tags: Array[String] = Array()

  def isEmpty() = {
    streamType == null
  }
}
