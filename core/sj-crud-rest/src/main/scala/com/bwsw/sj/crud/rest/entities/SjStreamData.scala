package com.bwsw.sj.crud.rest.entities

import com.bwsw.sj.common.StreamConstants
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "stream-type")
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamSjStreamData], name = StreamConstants.tStream),
  new Type(value = classOf[KafkaSjStreamData], name = StreamConstants.kafka),
  new Type(value = classOf[ESSjStreamData], name = StreamConstants.esOutput),
  new Type(value = classOf[JDBCSjStreamData], name = StreamConstants.jdbcOutput)
))
class SjStreamData() {
  @JsonProperty("stream-type") var streamType: String = null
  var name: String = null
  var description: String = null
  var service: String = null
  var tags: Array[String] = null
}

class TStreamSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.tStream
  var partitions: Int = 0
  var generator: GeneratorData = null
}

class KafkaSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.kafka
  var partitions: Int = 0
  @JsonProperty("replication-factor") var replicationFactor: Int = 0
}

class ESSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.esOutput
}

class JDBCSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.jdbcOutput
}
