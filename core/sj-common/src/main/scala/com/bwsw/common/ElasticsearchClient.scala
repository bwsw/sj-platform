package com.bwsw.common

import java.net.InetAddress

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

class ElasticsearchClient(hosts: Set[(String, Int)]) {

  private val client = TransportClient.builder().build()
  hosts.foreach(x => setTransportAddressToClient(x._1, x._2))

  def setTransportAddressToClient(host: String, port: Int) = {
    val transportAddress = new InetSocketTransportAddress(InetAddress.getByName(host), port)
    client.addTransportAddress(transportAddress)
  }

  def doesIndexExist(index: String) = {
    val indicesExistsResponse = client.admin().indices().prepareExists(index).execute().actionGet()

    indicesExistsResponse.isExists
  }

  def createIndex(index: String) = {
    client.admin().indices().prepareCreate(index).execute().actionGet()
  }

  def deleteIndex(index: String) = {
    client.admin().indices().prepareDelete(index).execute().actionGet()
  }

  def close() = {
    client.close()
  }
}
