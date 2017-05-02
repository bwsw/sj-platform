package com.bwsw.sj.common.DAL.model.stream

import com.bwsw.sj.common.DAL.model.service.Service
import com.bwsw.sj.common.rest.entities.stream.JDBCStreamData

class JDBCSjStream() extends SjStream {
  var primary: String = _
  def this(name: String,
           id: String,
           description: String,
           service: Service,
           streamType: String,
           tags: Array[String]) = {
    this()
    this.name = name
    this.primary = id
    this.description = description
    this.service = service
    this.streamType = streamType
    this.tags = tags
  }

  override def asProtocolStream() = {
    val streamData = new JDBCStreamData
    super.fillProtocolStream(streamData)

    streamData.primary = this.primary

    streamData
  }

  override def delete(): Unit = {}
}
