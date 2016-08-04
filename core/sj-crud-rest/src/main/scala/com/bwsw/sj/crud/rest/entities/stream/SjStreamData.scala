package com.bwsw.sj.crud.rest.entities.stream

import com.bwsw.sj.common.StreamConstants
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "stream-type")
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamSjStreamData], name = StreamConstants.tStreamType),
  new Type(value = classOf[KafkaSjStreamData], name = StreamConstants.kafkaStreamType),
  new Type(value = classOf[ESSjStreamData], name = StreamConstants.esOutputType),
  new Type(value = classOf[JDBCSjStreamData], name = StreamConstants.jdbcOutputType)
))
class SjStreamData() {
  @JsonProperty("stream-type") var streamType: String = null
  var name: String = null
  var description: String = null
  var service: String = null
  var tags: Array[String] = null
  var force: Boolean = false
}

class TStreamSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.tStreamType
  var partitions: Int = 0
  var generator: GeneratorData = null
}

class KafkaSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.kafkaStreamType
  var partitions: Int = 0
  @JsonProperty("replication-factor") var replicationFactor: Int = 0
}

class ESSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.esOutputType
}

class JDBCSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.jdbcOutputType
}
