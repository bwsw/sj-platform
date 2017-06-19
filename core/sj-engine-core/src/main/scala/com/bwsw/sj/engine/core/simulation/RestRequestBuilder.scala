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

import java.net.URI

import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.types.rest.RestCommandBuilder
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request

/**
  * Provides method for building HTTP POST request from [[OutputEnvelope]].
  *
  * @param url destination URL
  * @author Pavel Tomskikh
  */
class RestRequestBuilder(url: URI = RestRequestBuilder.defaultUrl) extends OutputRequestBuilder[Request] {

  override protected val commandBuilder = new RestCommandBuilder(transactionFieldName)
  private val client = new HttpClient

  /**
    * @inheritdoc
    */
  override def buildInsert(outputEnvelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[_]): Request =
    commandBuilder.buildInsert(inputEnvelope.id, outputEnvelope.getFieldsValue)(client.newRequest(url))

  /**
    * @inheritdoc
    */
  override def buildDelete(inputEnvelope: TStreamEnvelope[_]): Request =
    commandBuilder.buildDelete(inputEnvelope.id)(client.newRequest(url))
}

object RestRequestBuilder {
  val defaultUrl: URI = new URI("http://localhost/entity/")
}
