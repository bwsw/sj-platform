package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.service.{EsIndServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceConstants
import org.mongodb.morphia.annotations.Reference

class ESService() extends Service {
  serviceType = ServiceConstants.elasticsearchServiceType
  @Reference var provider: Provider = null
  var index: String = null
  var login: String = null
  var password: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, index: String, login: String, password: String) = {
    this()
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.index = index
    this.login = login
    this.password = password
  }

  override def toProtocolService(): ServiceData = {
    val protocolService = new EsIndServiceData()
    super.fillProtocolService(protocolService)

    protocolService.index = this.index
    protocolService.provider = this.provider.name
    protocolService.login = this.login
    protocolService.password = this.password

    protocolService
  }
}
