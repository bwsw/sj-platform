package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Embedded

class TStreamSjStream() extends SjStream {
  var partitions: Int = 0
  @Embedded var generator: Generator = null

  //todo Should we remove nested `this`? See TaskManager for its use, it may create non-valid streams in db
  def this(name: String,
           description: String,
           partitions: Int,
           generator: Generator) = {
    this()
    this.name = name
    this.description = description
    this.partitions = partitions
    this.generator = generator
  }

  def this(name: String,
           description: String,
           partitions: Int,
           service: Service,
           streamType: String,
           tags: Array[String],
           generator: Generator) = {
    this(name, description, partitions, generator)
    this.service = service
    this.streamType = streamType
    this.tags = tags
    this.generator = generator
  }
}
