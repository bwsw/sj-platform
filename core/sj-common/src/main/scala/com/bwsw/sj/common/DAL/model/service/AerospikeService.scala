package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.rest.entities.service.{ArspkDBServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals
import org.mongodb.morphia.annotations.Reference

class AerospikeService() extends Service {
  serviceType = ServiceLiterals.aerospikeType
  @Reference var provider: Provider = null
  var namespace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, namespace: String): Unit = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.namespace = namespace
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new ArspkDBServiceData()
    super.fillProtocolService(protocolService)

    protocolService.namespace = this.namespace
    protocolService.provider = this.provider.name

    protocolService
  }
}
