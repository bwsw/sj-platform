package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField}
import com.bwsw.sj.common.rest.model.service.ServiceData
import org.mongodb.morphia.annotations.Entity

@Entity("services")
class Service(@IdField val name: String,
              val description: String,
              @PropertyField("type") val serviceType: String)  {

  def prepare(): Unit = {}
  
  def destroy(): Unit = {}

  def asProtocolService(): ServiceData = ???
}
