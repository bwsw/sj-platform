package com.bwsw.sj.common.DAL.model.stream

import com.bwsw.sj.common.DAL.model.service.RestService
import com.bwsw.sj.common.rest.entities.stream.RestStreamData
import com.bwsw.sj.common.utils.StreamLiterals

/**
  * Stream for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestSjStream(override val name: String,
                   override val description: String,
                   override val service: RestService,
                   override val tags: Array[String],
                   override val force: Boolean,
                   override val streamType: String = StreamLiterals.restOutputType)
  extends SjStream(name, description, service, tags, force, streamType) {

  override def asProtocolStream() = {
    val streamData = new RestStreamData
    fillProtocolStream(streamData)

    streamData
  }

  override def delete() = {}
}
