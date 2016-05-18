package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations._

@Entity("streams")
class SjStream() {
  @Id var name: String = null
  var description: String = null
  var partitions: Int = 0
  @Reference var service: Service = null
  @Property("stream-type") var streamType: String = null
  var tags: Array[String] = null
  @Embedded var generator: Generator = null

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
