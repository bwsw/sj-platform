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
package com.bwsw.sj.engine.output.processing

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.dal.model.stream.ESStreamDomain
import com.bwsw.sj.common.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.output.Entity
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.core.output.types.es.ElasticsearchCommandBuilder
import com.bwsw.sj.engine.output.task.OutputTaskManager
import org.elasticsearch.action.bulk.BulkRequestBuilder
import scaldi.Injector

/**
  * ref. [[OutputProcessor]] object
  */
class EsOutputProcessor[T <: AnyRef](esStream: ESStreamDomain,
                                     performanceMetrics: PerformanceMetrics,
                                     manager: OutputTaskManager,
                                     entity: Entity[_])
                                    (implicit injector: Injector)
  extends AsyncOutputProcessor[T](esStream, performanceMetrics) {

  private val esService = esStream.service
  private val esClient = openConnection()
  private var maybeBulkRequestBuilder: Option[BulkRequestBuilder] = None
  private var inputEnvelopesInBulk: Long = 0
  private val maxInputEnvelopesPerBulk = Seq(manager.outputInstance.checkpointInterval / outputParallelism, 1L).max
  private var lastInputEnvelopeId: Long = 0
  override protected val commandBuilder: ElasticsearchCommandBuilder =
    new ElasticsearchCommandBuilder(transactionFieldName, entity.asInstanceOf[Entity[String]])

  private def openConnection(): ElasticsearchClient = {
    logger.info(s"Open a connection to elasticsearch at address: '${esService.provider.hosts}'.")
    val hosts = esService.provider.hosts.map(splitHost).toSet
    val maybeUsername = Option(esService.provider.login)
    val maybePassword = Option(esService.provider.password)

    new ElasticsearchClient(hosts, maybeUsername, maybePassword)
  }

  private def splitHost(host: String): (String, Int) = {
    val parts = host.split(":")

    (parts(0), parts(1).toInt)
  }

  def delete(envelope: TStreamEnvelope[T]): Unit = {
    val index = esService.index
    val streamName = esStream.name
    logger.debug(s"Delete a transaction: '${envelope.id}' from elasticsearch stream.")
    if (esClient.doesIndexExist(index)) {
      val query = commandBuilder.buildDelete(envelope.id)
      esClient.deleteDocuments(index, streamName, query)
    }
  }

  override def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit = {
    if (maybeBulkRequestBuilder.isEmpty)
      maybeBulkRequestBuilder = Some(esClient.createBulk())

    if (lastInputEnvelopeId != inputEnvelope.id) {
      lastInputEnvelopeId = inputEnvelope.id
      inputEnvelopesInBulk += 1
    }

    val bulkRequestBuilder = maybeBulkRequestBuilder.get
    val esFieldsValue = envelope.getFieldsValue
    val data = commandBuilder.buildInsert(inputEnvelope.id, esFieldsValue)
    logger.debug(s"Task: ${manager.taskName}. Write an output envelope to elasticsearch stream.")
    esClient.addToBulk(bulkRequestBuilder, data, esService.index, esStream.name)


    if (inputEnvelopesInBulk > maxInputEnvelopesPerBulk) sendBulk()
  }

  override def checkpoint(): Unit = {
    sendBulk()
    super.checkpoint()
  }

  override def close(): Unit = {
    esClient.close()
  }

  private def sendBulk(): Unit = {
    maybeBulkRequestBuilder match {
      case Some(bulkRequestBuilder) =>
        runInFuture(() => bulkRequestBuilder.get())
        inputEnvelopesInBulk = 0
        maybeBulkRequestBuilder = None

      case None =>
    }
  }
}
