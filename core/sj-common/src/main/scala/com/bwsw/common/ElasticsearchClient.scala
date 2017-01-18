package com.bwsw.common

import java.net.InetAddress
import java.util.UUID

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHits
import org.elasticsearch.transport.client.PreBuiltTransportClient


class ElasticsearchClient(hosts: Set[(String, Int)]){

  private val client = new PreBuiltTransportClient(Settings.EMPTY)
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

  def deleteDocumentByTypeAndId(index: String, documentType: String, documentId: String) = {
    client.prepareDelete(index, documentType, documentId).execute().actionGet()
  }

  def deleteIndex(index: String) = {
    client.admin().indices().prepareDelete(index).execute().actionGet()
  }

  def createMapping(index: String, mappingType: String, mappingSource: String) = {
    client.admin().indices()
      .preparePutMapping(index)
      .setType(mappingType)
      .setSource(mappingSource)
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

  def write(data: String, index: String, documentType: String, documentId: String = UUID.randomUUID().toString) = {
    client
      .prepareIndex(index, documentType, documentId)
      .setSource(data)
      .execute()
      .actionGet()
  }

  def isConnected() = {
    //val settings = Settings.settingsBuilder().put("client.transport.ping_timeout", "2s").build() todo maybe need
    client.connectedNodes().size() < 1
  }

  def close() = {
    client.close()
  }
}
