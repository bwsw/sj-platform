package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations._

@Entity("streams")
class SjStream() {
  @Id var name: String = null
  var description: String = null
  @Reference var service: Service = null
  @Property("stream-type") var streamType: String = null
  var tags: Array[String] = Array()

  def this(name: String,
           description: String,
           service: Service,
           streamType: String,
           tags: Array[String]) = {
    this()
    this.name = name
    this.description = description
    this.service = service
    this.streamType = streamType
    this.tags = tags
  }
}
