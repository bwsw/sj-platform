package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.ESStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scaldi.Injector

class ESStreamApi(name: String,
                  service: String,
                  tags: Option[Array[String]] = Some(Array()),
                  @JsonDeserialize(contentAs = classOf[Boolean]) force: Option[Boolean] = Some(false),
                  description: Option[String] = Some(RestLiterals.defaultDescription),
                  @JsonProperty("type") streamType: Option[String] = Some(StreamLiterals.esOutputType))
  extends StreamApi(streamType.getOrElse(StreamLiterals.esOutputType), name, service, tags, force, description) {

  override def to(implicit injector: Injector): ESStream =
    new ESStream(
      name,
      service,
      tags.getOrElse(Array()),
      force.getOrElse(false),
      streamType.getOrElse(StreamLiterals.esOutputType),
      description.getOrElse(RestLiterals.defaultDescription))
}
