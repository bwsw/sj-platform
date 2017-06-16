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

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import org.apache.http.entity.ContentType
import org.eclipse.jetty.http.HttpVersion

/**
  * Provides method for building HTTP POST request from [[OutputEnvelope]].
  *
  * @param url         destination URL
  * @param httpVersion version of HTTP
  * @author Pavel Tomskikh
  */
class RestRequestBuilder(url: URI = RestRequestBuilder.defaultUrl,
                         httpVersion: HttpVersion = RestRequestBuilder.defaultHttpVersion)
  extends OutputRequestBuilder {

  private val serializer = new JsonSerializer
  private val contentType = ContentType.APPLICATION_JSON.toString

  /**
    * @inheritdoc
    */
  override def build(outputEnvelope: OutputEnvelope,
                     inputEnvelope: TStreamEnvelope[_]): String = {
    val data = outputEnvelope.getFieldsValue + (transactionFieldName -> inputEnvelope.id)
    val serialized = serializer.serialize(data)

    s"""POST ${url.getPath} $httpVersion
       |Host: ${url.getHost}
       |Content-Type: $contentType
       |Content-Length: ${serialized.length}
       |
       |$serialized""".stripMargin
  }
}

object RestRequestBuilder {
  val defaultUrl: URI = new URI("http://localhost/entity/")
  val defaultHttpVersion: HttpVersion = HttpVersion.HTTP_1_1
}
