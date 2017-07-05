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

import java.net.URI

import com.bwsw.common.JsonSerializer
import org.apache.http.entity.ContentType
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.http.HttpMethod
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import scaldi.Module

import scala.collection.JavaConverters._
import scala.util.parsing.json.JSONObject

/**
  * Tests for [[RestCommandBuilder]]
  *
  * @author Pavel Tomskikh
  */
class RestCommandBuilderTest extends FlatSpec with Matchers with MockitoSugar with TableDrivenPropertyChecks {
  val transactionFieldName = "txn"
  val contentType = ContentType.APPLICATION_JSON.toString
  val jsonSerializer = mock[JsonSerializer]
  val client = new HttpClient
  val url = new URI("http://localhost/")

  val injector = new Module {
    bind[JsonSerializer] to jsonSerializer
  }

  "RestCommandBuilder" should "return correct request transformation for insert requests" in {
    val transaction = 123456789l
    val maps = Table(
      "map",
      Map(
        "field1" -> "value1",
        "field2" -> Map(
          "field3" -> true,
          "field4" -> 4)),
      Map.empty[String, Any])

    val restCommandBuilder = new RestCommandBuilder(transactionFieldName, contentType)(injector)
    forAll(maps) { map =>
      val mapWithTransaction = map + (transactionFieldName -> transaction)
      val serializedMap = JSONObject(mapWithTransaction).toString()
      when(jsonSerializer.serialize(mapWithTransaction)).thenReturn(serializedMap)

      val requestTransformation = restCommandBuilder.buildInsert(transaction, map)
      val request = requestTransformation(client.newRequest(url))

      val expectedRequest = client.newRequest(url).method(HttpMethod.POST)
        .content(new StringContentProvider(serializedMap), contentType)

      checkRequest(request, expectedRequest)
    }
  }

  it should "return correct request transformation for delete requests" in {
    val transaction = 123456789l
    val restCommandBuilder = new RestCommandBuilder(transactionFieldName, contentType)(injector)
    val requestTransformation = restCommandBuilder.buildDelete(transaction)
    val request = requestTransformation(client.newRequest(url))

    val expectedRequest = client.newRequest(url).method(HttpMethod.DELETE)
      .param(transactionFieldName, transaction.toString)

    checkRequest(request, expectedRequest)
  }


  def checkRequest(request: Request, expectedRequest: Request) = {
    request.getAgent shouldBe expectedRequest.getAgent
    request.getAttributes shouldBe expectedRequest.getAttributes

    if (expectedRequest.getContent != null) {
      request.getContent.getLength shouldBe expectedRequest.getContent.getLength
      request.getContent.iterator().asScala.toList shouldBe expectedRequest.getContent.iterator().asScala.toList
    } else
      request.getContent shouldBe null

    request.getCookies shouldBe expectedRequest.getCookies
    request.getHeaders shouldBe expectedRequest.getHeaders
    request.getHost shouldBe expectedRequest.getHost
    request.getIdleTimeout shouldBe expectedRequest.getIdleTimeout
    request.getMethod shouldBe expectedRequest.getMethod
    request.getParams shouldBe expectedRequest.getParams
    request.getPath shouldBe expectedRequest.getPath
    request.getPort shouldBe expectedRequest.getPort
    request.getQuery shouldBe expectedRequest.getQuery
    request.getScheme shouldBe expectedRequest.getScheme
    request.getTimeout shouldBe expectedRequest.getTimeout
    request.getURI shouldBe expectedRequest.getURI
    request.getVersion shouldBe expectedRequest.getVersion
  }
}
