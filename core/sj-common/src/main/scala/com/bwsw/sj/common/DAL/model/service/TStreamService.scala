package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.rest.entities.service.{ServiceData, TstrQServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals
import org.mongodb.morphia.annotations.Reference

class TStreamService() extends Service {
  serviceType = ServiceLiterals.tstreamsType
  @Reference var provider: Provider = null
  var prefix: String = null
  var token: String = null

  def this(name: String,
           serviceType: String,
           description: String,
           provider: Provider,
           prefix: String,
           token: String): Unit = {
    this()
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.prefix = prefix
    this.token = token
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new TstrQServiceData()
    super.fillProtocolService(protocolService)

    protocolService.provider = this.provider.name
    protocolService.prefix = this.prefix
    protocolService.token = this.token

    protocolService
  }
}
