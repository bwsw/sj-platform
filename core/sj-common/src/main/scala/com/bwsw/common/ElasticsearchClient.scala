package com.bwsw.common

import java.net.InetAddress
import java.util.UUID

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHits

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

  def deleteIndexDocumentById(index: String, documentType: String, id: String) = {
    client.prepareDelete(index, documentType, id).execute().actionGet()
  }

  def deleteIndex(index: String) = {
    client.admin().indices().prepareDelete(index).execute().actionGet()
  }

  def createMapping(index: String, mappingType: String, mappingDefinition: String) = {
    client.admin().indices()
      .preparePutMapping(index)
      .setType(mappingType)
      .setSource(mappingDefinition)
      .execute()
      .actionGet()
  }

  def search(index: String, documentType: String, queryBuilder: QueryBuilder = QueryBuilders.matchAllQuery()): SearchHits = {
    client
      .prepareSearch(index)
      .setTypes(documentType)
      .setQuery(queryBuilder)
      .setSize(2000)
      .execute().get()
      .getHits
  }

  def writeWithRandomId(index: String, documentType: String, data: String) = {
    client
      .prepareIndex(index, documentType, UUID.randomUUID().toString)
      .setSource(data)
      .execute()
      .actionGet()
  }

  def close() = {
    client.close()
  }
}
