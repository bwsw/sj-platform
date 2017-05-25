package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.JDBCStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

class JDBCStreamApi(val primary: String,
                    name: String,
                    service: String,
                    tags: Option[Array[String]] = Some(Array()),
                    @JsonDeserialize(contentAs = classOf[Boolean]) force: Option[Boolean] = Some(false),
                    description: Option[String] = Some(RestLiterals.defaultDescription),
                    @JsonProperty("type") streamType: Option[String] = Some(StreamLiterals.jdbcOutputType))
  extends StreamApi(streamType.getOrElse(StreamLiterals.jdbcOutputType), name, service, tags, force, description) {

  override def to: JDBCStream =
    new JDBCStream(
      name,
      service,
      primary,
      tags.getOrElse(Array()),
      force.getOrElse(false),
      streamType.getOrElse(StreamLiterals.jdbcOutputType),
      description.getOrElse(RestLiterals.defaultDescription))
}
