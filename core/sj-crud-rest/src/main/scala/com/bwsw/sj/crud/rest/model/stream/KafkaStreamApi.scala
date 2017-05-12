package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.KafkaStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class KafkaStreamApi(name: String,
                     service: String,
                     tags: Array[String] = Array(),
                     force: Boolean = false,
                     description: String = RestLiterals.defaultDescription,
                     val partitions: Int = Int.MinValue,
                     val replicationFactor: Int = Int.MinValue,
                     @JsonProperty("type") streamType: String = StreamLiterals.restOutputType)
  extends StreamApi(streamType, name, service, tags, force, description) {

  override def to: KafkaStream = new KafkaStream(
    name,
    service,
    partitions,
    replicationFactor,
    tags,
    force,
    streamType,
    description)
}
