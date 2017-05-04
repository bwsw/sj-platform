package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.rest.entities.service.ServiceData
import org.mongodb.morphia.annotations.{Entity, Id, Property}

@Entity("services")
class Service() {
  @Id var name: String = null
  @Property("type") var serviceType: String = null
  var description: String = null

  def this(name: String, serviceType: String, description: String): Unit = {
    this()
    this.name = name
    this.serviceType = serviceType
    this.description = description
  }

  def prepare(): Unit = {}
  
  def destroy(): Unit = {}

  def asProtocolService(): ServiceData = ???

  protected def fillProtocolService(protocolService: ServiceData): Unit = {
    protocolService.name = this.name
    protocolService.description = this.description
    protocolService.serviceType = this.serviceType
  }
}
