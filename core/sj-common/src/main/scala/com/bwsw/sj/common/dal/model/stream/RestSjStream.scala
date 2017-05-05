package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.RestService
import com.bwsw.sj.common.rest.model.stream.RestStreamData
import com.bwsw.sj.common.utils.StreamLiterals

/**
  * Stream for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestSjStream(override val name: String,
                   override val service: RestService,
                   override val description: String = "No description",
                   override val force: Boolean = false,
                   override val tags: Array[String] = Array(),
                   override val streamType: String = StreamLiterals.restOutputType)
  extends SjStream(name, description, service, force, tags, streamType) {

  override def asProtocolStream() = {
    val streamData = new RestStreamData
    fillProtocolStream(streamData)

    streamData
  }

  override def delete() = {}
}
