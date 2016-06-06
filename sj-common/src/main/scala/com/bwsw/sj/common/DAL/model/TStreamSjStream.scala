package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Embedded

class TStreamSjStream() extends SjStream {
  var partitions: Int = 0
  @Embedded var generator: Generator = null

  def this(name: String,
           description: String,
           partitions: Int,
           service: Service,
           streamType: String,
           tags: Array[String],
           generator: Generator) = {
    this()
    this.name = name
    this.description = description
    this.partitions = partitions
    this.service = service
    this.streamType = streamType
    this.tags = tags
    this.generator = generator
  }
}
