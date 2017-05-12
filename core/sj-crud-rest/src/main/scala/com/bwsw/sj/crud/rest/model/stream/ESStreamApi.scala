package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.ESStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class ESStreamApi(name: String,
                  service: String,
                  tags: Array[String] = Array(),
                  force: Boolean = false,
                  description: String = RestLiterals.defaultDescription,
                  @JsonProperty("type") streamType: String = StreamLiterals.restOutputType)
  extends StreamApi(streamType, name, service, tags, force, description) {

  override def to: ESStream =
    new ESStream(name, service, tags, force, streamType, description)
}
