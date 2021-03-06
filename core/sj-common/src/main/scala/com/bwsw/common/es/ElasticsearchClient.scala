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

import com.bwsw.sj.common.utils.ProviderLiterals
import com.typesafe.scalalogging.Logger
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse
import org.elasticsearch.action.bulk.BulkRequestBuilder
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentType}
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.index.reindex.{BulkByScrollResponse, DeleteByQueryAction}
import org.elasticsearch.search.SearchHits
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient

/**
  * Wrapper for [[org.elasticsearch.client.transport.TransportClient]]
  *
  * @param hosts    es address
  * @param username es username
  * @param password es password
  */
class ElasticsearchClient(hosts: Set[(String, Int)],
                          username: Option[String] = None,
                          password: Option[String] = None) {
  private val logger = Logger(this.getClass)
  private val typeName = "_type"
  System.setProperty("es.set.netty.runtime.available.processors", "false") //to avoid the following exception
  // Exception in thread "output-task-pingstation-output-task0-engine" java.lang.IllegalStateException:
  // availableProcessors is already set to [4], rejecting [4] (we don't know it's a bug or not)
  private val settingsBuilder = Settings.builder()
  settingsBuilder.put("transport.tcp.connect_timeout", ProviderLiterals.connectTimeoutMillis + "ms")
  username.zip(password).foreach {
    case (u, p) => settingsBuilder.put("xpack.security.user", s"$u:$p")
  }

  private val client = new PreBuiltXPackTransportClient(settingsBuilder.build())
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
                      query: String = QueryBuilders.matchAllQuery().toString): BulkByScrollResponse = {
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
      .setSource(data, XContentType.JSON)
      .execute()
      .actionGet()
  }

  /**
    * Adds data into bulk builder
    *
    * @param bulkRequestBuilder bulk builder
    * @param data               document data
    * @param index              index
    * @param documentType       document type
    * @param documentId         document id
    * @return bulk builder
    */
  def addToBulk(bulkRequestBuilder: BulkRequestBuilder,
                data: String,
                index: String,
                documentType: String,
                documentId: String = UUID.randomUUID().toString): BulkRequestBuilder = {
    logger.debug(s"Write a data: '$data' to an elasticsearch index: '$index'.")

    bulkRequestBuilder.add(
      client
        .prepareIndex(index, documentType, documentId)
        .setSource(data, XContentType.JSON))
  }

  /**
    * Creates new bulk builder
    *
    * @return bulk builder
    */
  def createBulk(): BulkRequestBuilder =
    client.prepareBulk()

  def isConnected(): Boolean = {
    logger.debug(s"Check a connection to an elasticsearch database.")
    client.connectedNodes().size() > 0
  }

  def close(): Unit = {
    logger.info(s"Close an elasticsearch database connection.")
    client.close()
  }
}