package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.rest.model.stream.RestStreamApi
import com.bwsw.sj.common.utils.StreamLiterals

/**
  * Stream for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestStreamDomain(override val name: String,
                       override val service: RestServiceDomain,
                       override val description: String = "No description",
                       override val force: Boolean = false,
                       override val tags: Array[String] = Array(),
                       override val streamType: String = StreamLiterals.restOutputType)
  extends StreamDomain(name, description, service, force, tags, streamType) {

  override def asProtocolStream(): RestStreamApi = {
    val streamData = new RestStreamApi
    fillProtocolStream(streamData)

    streamData
  }

  override def delete(): Unit = {}
}
