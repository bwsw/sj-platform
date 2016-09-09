package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.service.{CassDBServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceConstants
import org.mongodb.morphia.annotations.Reference

class CassandraService() extends Service {
  serviceType = ServiceConstants.cassandraServiceType
  @Reference var provider: Provider = null
  var keyspace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, keyspace: String) = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.keyspace = keyspace
  }

  override def toProtocolService(): ServiceData = {
    val protocolService = new CassDBServiceData()
    super.fillProtocolService(protocolService)

    protocolService.keyspace = this.keyspace
    protocolService.provider = this.provider.name

    protocolService
  }
}
