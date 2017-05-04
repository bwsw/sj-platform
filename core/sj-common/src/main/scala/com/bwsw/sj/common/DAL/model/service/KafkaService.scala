package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.rest.entities.service.{KfkQServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals
import org.mongodb.morphia.annotations.{Property, Reference}

class KafkaService() extends Service {
  serviceType = ServiceLiterals.kafkaType
  @Reference var provider: Provider = null
  @Reference(value = "zk-provider") var zkProvider: Provider = null
  @Property("zk-namespace") var zkNamespace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, zkProvider: Provider, zkNamespace: String): Unit = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.zkProvider = zkProvider
    this.zkNamespace = zkNamespace
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new KfkQServiceData()
    super.fillProtocolService(protocolService)

    protocolService.provider = this.provider.name
    protocolService.zkProvider = this.zkProvider.name
    protocolService.zkNamespace = this.zkNamespace

    protocolService
  }
}
