/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.common.es

import java.net.InetAddress
import java.util.UUID

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.index.reindex.{BulkIndexByScrollResponse, DeleteByQueryAction}
import org.elasticsearch.search.SearchHits
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.slf4j.LoggerFactory

/**
  * Wrapper for [[TransportClient]]
  *
  * @param hosts es address
  */
class ElasticsearchClient(hosts: Set[(String, Int)]) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val typeName = "_type"
  private val client = new PreBuiltTransportClient(Settings.EMPTY)
  hosts.foreach(x => setTransportAddressToClient(x._1, x._2))
  private val deleteByQueryAction = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)

  private def setTransportAddressToClient(host: String, port: Int): TransportClient = {
    logger.debug(s"Add a new transport address: '$host:$port' to an elasticsearch client.")
    val transportAddress = new InetSocketTransportAddress(InetAddress.getByName(host), port)
    client.addTransportAddress(transportAddress)
  }

  def doesIndexExist(index: String): Boolean = {
    logger.debug(s"Verify the existence of an elasticsearch index: '$index'.")
    val indicesExistsResponse = client.admin().indices().prepareExists(index).execute().actionGet()

    indicesExistsResponse.isExists
  }

  def createIndex(index: String): CreateIndexResponse = {
    logger.info(s"Create a new index: '$index' in Elasticsearch.")
    client.admin().indices().prepareCreate(index).execute().actionGet()
  }

  def deleteDocuments(index: String,
                      documentType: String,
                      query: String = QueryBuilders.matchAllQuery().toString): BulkIndexByScrollResponse = {
    val queryWithType = new BoolQueryBuilder()
      .must(QueryBuilders.wrapperQuery(query))
      .must(QueryBuilders.matchQuery(typeName, documentType))

    deleteByQueryAction
      .filter(queryWithType)
      .source(index)
      .get()
  }

  def deleteIndex(index: String): DeleteIndexResponse = {
    logger.info(s"Delete an index: '$index' from Elasticsearch.")
    client.admin().indices().prepareDelete(index).execute().actionGet()
  }

  def createMapping(index: String, mappingType: String, mappingSource: XContentBuilder): PutMappingResponse = {
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

  def write(data: String, index: String, documentType: String, documentId: String = UUID.randomUUID().toString): IndexResponse = {
    logger.debug(s"Write a data: '$data' to an elasticsearch index: '$index'.")
    client
      .prepareIndex(index, documentType, documentId)
      .setSource(data)
      .execute()
      .actionGet()
  }

  def isConnected(): Boolean = {
    logger.debug(s"Check a connection to an elasticsearch database.")
    client.connectedNodes().size() > 0
  }

  def close(): Unit = {
    logger.info(s"Close an elasticsearch database connection.")
    client.close()
  }
}