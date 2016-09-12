package com.bwsw.sj.common.DAL.model

import java.net.InetAddress

import com.bwsw.sj.common.rest.entities.service.{EsIndServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceConstants
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
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

  override def asProtocolService(): ServiceData = {
    val protocolService = new EsIndServiceData()
    super.fillProtocolService(protocolService)

    protocolService.index = this.index
    protocolService.provider = this.provider.name
    protocolService.login = this.login
    protocolService.password = this.password

    protocolService
  }

  override def prepare() = {
    val client = createClient()

    if (!doesIndexExist(client)) {
      createIndex(client)
    }
  }

  override def destroy() = {
    val client = createClient()
    deleteIndex(client)
  }

  private def createClient() = {
    val client = TransportClient.builder().build()
    setTransportAddresses(client)

    client
  }

  private def setTransportAddresses(client: TransportClient) = {
    this.provider.hosts.foreach(host => setTransportAddressToClient(host, client))
  }

  private def setTransportAddressToClient(host: String, client: TransportClient) = {
    val hostAndPort = host.split(":")
    val transportAddress = new InetSocketTransportAddress(InetAddress.getByName(hostAndPort(0)), hostAndPort(1).toInt)
    client.addTransportAddress(transportAddress)
  }

  private def doesIndexExist(client: TransportClient) = {
    val indicesExistsResponse = client.admin().indices().prepareExists(this.index).execute().actionGet()

    indicesExistsResponse.isExists
  }

  private def createIndex(client: TransportClient) = {
    client.admin().indices().prepareCreate(this.index).execute().actionGet()
  }

  private def deleteIndex(client: TransportClient) = {
    client.admin().indices().prepareDelete(this.index).execute().actionGet()
  }
}
