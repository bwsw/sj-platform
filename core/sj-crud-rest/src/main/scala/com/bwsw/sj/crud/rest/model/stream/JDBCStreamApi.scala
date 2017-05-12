package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.JDBCStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class JDBCStreamApi(val primary: String,
                    name: String,
                    service: String,
                    tags: Array[String] = Array(),
                    force: Boolean = false,
                    description: String = RestLiterals.defaultDescription,
                    @JsonProperty("type") streamType: String = StreamLiterals.restOutputType)
  extends StreamApi(streamType, name, service, tags, force, description) {

  override def to: JDBCStream =
    new JDBCStream(name, service, primary, tags, force, streamType, description)
}
