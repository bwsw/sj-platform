package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.KafkaStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

class KafkaStreamApi(name: String,
                     service: String,
                     tags: Option[Array[String]] = Some(Array()),
                     @JsonDeserialize(contentAs = classOf[Boolean]) force: Option[Boolean] = Some(false),
                     description: Option[String] = Some(RestLiterals.defaultDescription),
                     @JsonDeserialize(contentAs = classOf[Int]) val partitions: Option[Int] = Some(Int.MinValue),
                     @JsonDeserialize(contentAs = classOf[Int]) val replicationFactor: Option[Int] = Some(Int.MinValue),
                     @JsonProperty("type") streamType: Option[String] = Some(StreamLiterals.kafkaStreamType))
  extends StreamApi(streamType.getOrElse(StreamLiterals.kafkaStreamType), name, service, tags, force, description) {

  override def to: KafkaStream = new KafkaStream(
    name,
    service,
    partitions.getOrElse(Int.MinValue),
    replicationFactor.getOrElse(Int.MinValue),
    tags.getOrElse(Array()),
    force.getOrElse(false),
    streamType.getOrElse(StreamLiterals.kafkaStreamType),
    description.getOrElse(RestLiterals.defaultDescription))
}
