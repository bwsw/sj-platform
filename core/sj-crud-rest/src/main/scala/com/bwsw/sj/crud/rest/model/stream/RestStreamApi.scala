package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.RestStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

class RestStreamApi(name: String,
                    service: String,
                    tags: Option[Array[String]] = Some(Array()),
                    @JsonDeserialize(contentAs = classOf[Boolean]) force: Option[Boolean] = Some(false),
                    description: Option[String] = Some(RestLiterals.defaultDescription),
                    @JsonProperty("type") streamType: Option[String] = Some(StreamLiterals.restOutputType))
  extends StreamApi(streamType.getOrElse(StreamLiterals.restOutputType), name, service, tags, force, description) {

  override def to: RestStream =
    new RestStream(
      name,
      service,
      tags.getOrElse(Array()),
      force.getOrElse(false),
      streamType.getOrElse(StreamLiterals.restOutputType),
      description.getOrElse(RestLiterals.defaultDescription))
}
