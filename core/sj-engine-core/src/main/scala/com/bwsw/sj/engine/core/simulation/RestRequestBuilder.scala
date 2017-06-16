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
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.http.HttpVersion

import scala.collection.JavaConverters._

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

  override protected val commandBuilder = new RestCommandBuilder(transactionFieldName)
  private val client = new HttpClient

  /**
    * @inheritdoc
    */
  override def build(outputEnvelope: OutputEnvelope,
                     inputEnvelope: TStreamEnvelope[_]): String = {
    val request = commandBuilder.buildInsert(inputEnvelope.id, outputEnvelope.getFieldsValue)(client.newRequest(url))
    val content = request.getContent.asInstanceOf[StringContentProvider]
    val contentIterator = content.iterator().asScala
    val data = contentIterator.map(byteBuffer => new String(byteBuffer.array())).mkString("")

    s"""POST ${request.getPath} ${request.getVersion}
       |Host: ${request.getHost}
       |Content-Type: ${content.getContentType}
       |Content-Length: ${content.getLength}
       |
       |$data""".stripMargin
  }
}

object RestRequestBuilder {
  val defaultUrl: URI = new URI("http://localhost/entity/")
  val defaultHttpVersion: HttpVersion = HttpVersion.HTTP_1_1
}
