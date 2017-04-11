package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.service.{CassDBServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals
import org.mongodb.morphia.annotations.Reference

class CassandraService() extends Service {
  serviceType = ServiceLiterals.cassandraType
  @Reference var provider: Provider = null
  var keyspace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, keyspace: String) = {
    this()
    this.name = name
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

  private def isKeyspaceUsed = {
    val serviceManager = ConnectionRepository.getServiceManager

    val isKeyspaceUsedByCassandra = serviceManager.getByParameters(Map("type" -> this.serviceType))
      .map(x => x.asInstanceOf[CassandraService])
      .exists(x => x.keyspace == this.keyspace && x.name != this.name)

    val isKeyspaceUsedByTstreams = serviceManager.getByParameters(Map("type" -> ServiceLiterals.tstreamsType))
      .map(x => x.asInstanceOf[TStreamService])
      .exists(isKeyspaceUsedByTstreamService)

    isKeyspaceUsedByCassandra || isKeyspaceUsedByTstreams
  }

  private def isKeyspaceUsedByTstreamService(tStreamService: TStreamService): Boolean = {
//    tStreamService.metadataProvider == this.provider && tStreamService.metadataNamespace == this.keyspace ||
//      tStreamService.dataProvider == this.provider && tStreamService.dataNamespace == this.keyspace
    true //todo after integration with t-streams
  }
}
