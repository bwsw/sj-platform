package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations._

@Entity("streams")
class SjStream() {
  @Id var name: String = null
  var description: String = null
  var partitions: Int = 0
  @Reference var service: Service = null
  @Property("stream-type") var streamType: String = null
  var tags: String = null
  @Embedded var generator: Generator = null

  def this(name: String,
           description: String,
           partitions: Int,
           service: Service,
           streamType: String,
           tags: String,
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
