package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.stream.RestStreamData

/**
  * Stream for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestSjStream extends SjStream {

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

  override def asProtocolStream() = {
    val streamData = new RestStreamData
    fillProtocolStream(streamData)

    streamData
  }

  override def delete() = {}
}
