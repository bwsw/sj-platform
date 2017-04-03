package com.bwsw.common

import java.net.InetAddress
import java.util.UUID

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHits
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.slf4j.LoggerFactory


class ElasticsearchClient(hosts: Set[(String, Int)]) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val client = new PreBuiltTransportClient(Settings.EMPTY)
  hosts.foreach(x => setTransportAddressToClient(x._1, x._2))

  def setTransportAddressToClient(host: String, port: Int) = {
    logger.debug(s"Add a new transport address: '$host:$port' to an elasticsearch client.")
    val transportAddress = new InetSocketTransportAddress(InetAddress.getByName(host), port)
    client.addTransportAddress(transportAddress)
  }

  def doesIndexExist(index: String) = {
    logger.debug(s"Verify the existence of an elasticsearch index: '$index'.")
    val indicesExistsResponse = client.admin().indices().prepareExists(index).execute().actionGet()

    indicesExistsResponse.isExists
  }

  def createIndex(index: String) = {
    logger.info(s"Create a new index: '$index' in Elasticsearch.")
    client.admin().indices().prepareCreate(index).execute().actionGet()
  }

  def deleteDocumentByTypeAndId(index: String, documentType: String, documentId: String) = {
    logger.debug(s"Delete a document from index: '$index' by id: '$documentId'.")
    client.prepareDelete(index, documentType, documentId).execute().actionGet()
  }

  def deleteIndex(index: String) = {
    logger.info(s"Delete an index: '$index' from Elasticsearch.")
    client.admin().indices().prepareDelete(index).execute().actionGet()
  }

  def createMapping(index: String, mappingType: String, mappingSource: XContentBuilder) = {
    logger.debug(s"Create a new index: '$index' in Elasticsearch.")
    client.admin().indices()
      .preparePutMapping(index)
      .setType(mappingType)
      .setSource(mappingSource)
      .execute()
      .actionGet()
  }

  def search(index: String, documentType: String, queryBuilder: QueryBuilder = QueryBuilders.matchAllQuery()): SearchHits = {
    logger.debug(s"Search the documents by document type: '$documentType' and a query (all by default) in elasticsearch index: '$index'.")
    client
      .prepareSearch(index)
      .setTypes(documentType)
      .setQuery(queryBuilder)
      .setSize(2000)
      .execute().get()
      .getHits
  }

  def write(data: String, index: String, documentType: String, documentId: String = UUID.randomUUID().toString) = {
    logger.debug(s"Write a data: '$data' to an elasticsearch index: '$index'.")
    client
      .prepareIndex(index, documentType, documentId)
      .setSource(data)
      .execute()
      .actionGet()
  }

  def isConnected() = {
    logger.debug(s"Check a connection to an elasticsearch database.")
    client.connectedNodes().size() < 1
  }

  def close() = {
    logger.info(s"Close an elasticsearch database connection.")
    client.close()
  }
}
