package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.stream.KafkaSjStreamData

class KafkaSjStream() extends SjStream {

  var partitions: Int = 0
  var replicationFactor: Int = 0

  def this(name: String,
           description: String,
           partitions: Int,
           service: Service,
           streamType: String,
           tags: Array[String],
           replicationFactor: Int) = {
    this()
    this.name = name
    this.description = description
    this.partitions = partitions
    this.service = service
    this.streamType = streamType
    this.tags = tags
    this.replicationFactor = replicationFactor
  }

  override def asProtocolStream() = {
    val streamData = new KafkaSjStreamData
    super.fillProtocolStream(streamData)

    streamData.partitions = this.partitions
    streamData.replicationFactor = this.replicationFactor

    streamData
  }
}
