package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.{IdField, PropertyField}
import com.bwsw.sj.common.rest.entities.service.ServiceData
import org.mongodb.morphia.annotations.Entity

@Entity("services")
class Service(@IdField val name: String,
              val description: String,
              @PropertyField("type") val serviceType: String)  {

  def prepare(): Unit = {}
  
  def destroy(): Unit = {}

  def asProtocolService(): ServiceData = ???
}
