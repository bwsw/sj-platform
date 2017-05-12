package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.TStreamStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class TStreamStreamApi(name: String,
                       service: String,
                       tags: Array[String] = Array(),
                       force: Boolean = false,
                       description: String = RestLiterals.defaultDescription,
                       val partitions: Int = Int.MinValue,
                       @JsonProperty("type") streamType: String = StreamLiterals.restOutputType)
  extends StreamApi(streamType, name, service, tags, force, description) {

  override def to: TStreamStream =
    new TStreamStream(name, service, partitions, tags, force, streamType, description)
}
