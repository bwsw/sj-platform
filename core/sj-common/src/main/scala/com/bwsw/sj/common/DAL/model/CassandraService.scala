package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.service.{CassDBServiceData, ServiceData}
import com.bwsw.sj.common.utils.{CassandraFactory, Service}
import org.mongodb.morphia.annotations.Reference

class CassandraService() extends Service {
  serviceType = Service.cassandraType
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

  override def asProtocolService(): ServiceData = {
    val protocolService = new CassDBServiceData()
    super.fillProtocolService(protocolService)

    protocolService.keyspace = this.keyspace
    protocolService.provider = this.provider.name

    protocolService
  }

  override def prepare() = {
    val cassandraFactory = new CassandraFactory
    cassandraFactory.open(this.provider.getHosts())
    cassandraFactory.createKeyspace(this.keyspace)
    cassandraFactory.close()
  }

  override def destroy() = {
    val cassandraFactory = new CassandraFactory
    cassandraFactory.open(this.provider.getHosts())
    cassandraFactory.dropKeyspace(this.keyspace)
    cassandraFactory.close()
  }
}
