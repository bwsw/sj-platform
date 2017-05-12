package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.KafkaStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

class KafkaStreamApi(override val name: String,
                     override val service: String,
                     override val tags: Array[String] = Array(),
                     override val force: Boolean = false,
                     override val description: String = RestLiterals.defaultDescription,
                     val partitions: Int = Int.MinValue,
                     val replicationFactor: Int = Int.MinValue)
  extends StreamApi(StreamLiterals.kafkaStreamType, name, service, tags, force, description) {

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
