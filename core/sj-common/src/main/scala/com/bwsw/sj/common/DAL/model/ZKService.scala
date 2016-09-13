package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.service.{ServiceData, ZKCoordServiceData}
import com.bwsw.sj.common.utils.Service
import org.mongodb.morphia.annotations.Reference

class ZKService() extends Service {
  serviceType = Service.zookeeperType
  @Reference var provider: Provider = null
  var namespace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, namespace: String) = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.namespace = namespace
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new ZKCoordServiceData()
    super.fillProtocolService(protocolService)

    protocolService.namespace = this.namespace
    protocolService.provider = this.provider.name

    protocolService
  }
}
