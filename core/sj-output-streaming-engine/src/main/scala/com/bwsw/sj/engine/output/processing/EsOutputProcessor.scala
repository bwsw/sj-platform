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
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.es.ElasticsearchCommandBuilder
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics

/**
  * ref. [[OutputProcessor]] object
  */
class EsOutputProcessor[T <: AnyRef](esStream: ESStreamDomain,
                                     performanceMetrics: OutputStreamingPerformanceMetrics,
                                     manager: OutputTaskManager,
                                     entity: Entity[_])
  extends OutputProcessor[T](esStream, performanceMetrics) {

  private val esService = esStream.service
  private val esClient = openConnection()
  override protected val commandBuilder: ElasticsearchCommandBuilder =
    new ElasticsearchCommandBuilder(transactionFieldName, entity.asInstanceOf[Entity[String]])

  private def openConnection(): ElasticsearchClient = {
    logger.info(s"Open a connection to elasticsearch at address: '${esService.provider.hosts}'.")
    val hosts = esService.provider.hosts.map(splitHost).toSet
    new ElasticsearchClient(hosts)
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


  def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit = {
    val esFieldsValue = envelope.getFieldsValue
    val data = commandBuilder.buildInsert(inputEnvelope.id, esFieldsValue)
    logger.debug(s"Task: ${manager.taskName}. Write an output envelope to elasticsearch stream.")
    esClient.write(data, esService.index, esStream.name)
  }

  override def close(): Unit = {
    esClient.close()
  }
}
