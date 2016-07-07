package com.bwsw.sj.common.DAL.model

class JDBCSjStream() extends SjStream {
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
