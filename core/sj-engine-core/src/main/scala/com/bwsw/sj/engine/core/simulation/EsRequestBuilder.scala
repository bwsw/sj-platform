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
package com.bwsw.sj.engine.core.simulation

import java.util.UUID

import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.es.ElasticsearchCommandBuilder

/**
  * Provides method for building Elasticasearch query from [[OutputEnvelope]].
  *
  * @param outputEntity working entity gotten from
  *                     [[com.bwsw.sj.engine.core.output.OutputStreamingExecutor OutputStreamingExecutor]]
  * @param index        elasticsearch index name
  * @param documentType elasticsearch document type
  * @author Pavel Tomskikh
  */
class EsRequestBuilder(outputEntity: Entity[String],
                       index: String = EsRequestBuilder.defaultIndex,
                       documentType: String = EsRequestBuilder.defaultDocumentType)
  extends OutputRequestBuilder {

  override protected val commandBuilder: ElasticsearchCommandBuilder =
    new ElasticsearchCommandBuilder(transactionFieldName, outputEntity)

  /**
    * @inheritdoc
    */
  override def build(outputEnvelope: OutputEnvelope,
                     inputEnvelope: TStreamEnvelope[_]): String = {
    val data = commandBuilder.buildInsert(inputEnvelope.id, outputEnvelope.getFieldsValue)
    val documentId = UUID.randomUUID().toString

    s"""PUT /$index/$documentType/$documentId
       |$data""".stripMargin
  }
}

object EsRequestBuilder {
  val defaultIndex = "index"
  val defaultDocumentType = "entity"
}
