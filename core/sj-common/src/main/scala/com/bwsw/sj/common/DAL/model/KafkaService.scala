package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.service.{KfkQServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceConstants
import org.mongodb.morphia.annotations.Reference

class KafkaService() extends Service {
  serviceType = ServiceConstants.kafkaServiceType
  @Reference var provider: Provider = null
  @Reference var zkProvider: Provider = null
  var zkNamespace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, zkProvider: Provider, zkNamespace: String) = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.zkProvider = zkProvider
    this.zkNamespace = zkNamespace
  }

  override def toProtocolService(): ServiceData = {
    val protocolService = new KfkQServiceData()
    super.fillProtocolService(protocolService)

    protocolService.provider = this.provider.name
    protocolService.zkProvider = this.zkProvider.name
    protocolService.zkNamespace = this.zkNamespace

    protocolService
  }
}
