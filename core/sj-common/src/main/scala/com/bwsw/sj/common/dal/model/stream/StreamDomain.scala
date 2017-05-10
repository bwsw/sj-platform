package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField, ReferenceField}
import com.bwsw.sj.common.rest.model.stream.StreamApi
import org.mongodb.morphia.annotations._

@Entity("streams")
class StreamDomain(@IdField val name: String,
                   val description: String,
                   @ReferenceField val service: ServiceDomain,
                   val force: Boolean,
                   val tags: Array[String],
                   @PropertyField("stream-type") val streamType: String) {

  def create(): Unit = ???

  def delete(): Unit = ???

  def asProtocolStream(): StreamApi = ???

  protected def fillProtocolStream(stream: StreamApi) = {
    stream.name = this.name
    stream.description = this.description
    stream.service = this.service.name
    stream.streamType = this.streamType
    stream.tags = this.tags
    stream.force = this.force
  }
}
