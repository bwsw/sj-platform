package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField, ReferenceField}
import org.mongodb.morphia.annotations._

@Entity("streams")
class StreamDomain(@IdField val name: String,
                   val description: String,
                   @ReferenceField val service: ServiceDomain,
                   val force: Boolean,
                   val tags: Array[String],
                   @PropertyField("stream-type") val streamType: String) {

  def create(): Unit = {}
}
