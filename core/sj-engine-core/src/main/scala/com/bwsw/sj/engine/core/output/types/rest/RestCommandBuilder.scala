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
package com.bwsw.sj.engine.core.output.types.rest

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.engine.core.output.types.CommandBuilder
import org.apache.http.entity.ContentType
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.http.HttpMethod

/**
  * Provides methods for building http requests to CRUD data
  *
  * @param transactionFieldName name of transaction field to check data on duplicate
  * @param contentType          content-type HTTP field
  * @author Pavel Tomskikh
  */
class RestCommandBuilder(transactionFieldName: String,
                         contentType: String = ContentType.APPLICATION_JSON.toString)
  extends CommandBuilder {

  private val serializer = new JsonSerializer

  def buildInsert(transaction: Long, values: Map[String, Any]): (Request) => Request = {
    val entity = values + (transactionFieldName -> transaction)
    val data = serializer.serialize(entity)

    (request: Request) =>
      request.method(HttpMethod.POST).content(new StringContentProvider(data), contentType)
  }

  def buildDelete(transaction: Long) = {
    (request: Request) =>
      request.method(HttpMethod.DELETE).param(transactionFieldName, transaction.toString)
  }
}
