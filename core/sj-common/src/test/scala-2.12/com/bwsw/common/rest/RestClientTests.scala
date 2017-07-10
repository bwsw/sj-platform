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
package com.bwsw.common.rest

import com.google.common.io.BaseEncoding
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.http.HttpMethod._
import org.eclipse.jetty.http.HttpVersion._
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.HttpStatusCode._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}


/**
  * Tests for [[RestClient]]
  *
  * @author Pavel Tomskikh
  */
class RestClientTests extends FlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with TableDrivenPropertyChecks {
  val server = new ClientAndServer()
  val port = server.getPort
  val hosts = Set(s"localhost:$port")
  val responseOk200 = response().withStatusCode(OK_200.code())

  override def afterEach(): Unit = server.reset()

  "RestClient" should "send correct requests" in {
    val path = "/some/path"

    def partialRequest: HttpRequest =
      HttpRequest.request(path)

    val contentData = "content data"
    val requestTransformations = Table(
      ("requestTransformation", "expectedRequest"),
      ((r: Request) => r.method(GET), partialRequest.withMethod(GET.asString())),
      ((r: Request) => r.method(POST).content(new StringContentProvider(contentData)),
        partialRequest.withMethod(POST.asString()).withBody(contentData)),
      ((r: Request) => r.method(DELETE), partialRequest.withMethod(DELETE.asString())))

    val client = new RestClient(hosts, path, HTTP_1_1, Map.empty[String, String])

    forAll(requestTransformations) { (requestTransformation, expectedRequest) =>
      server.when(expectedRequest).respond(responseOk200)
      val isSuccess = client.execute(requestTransformation)
      server.verify(expectedRequest)
      isSuccess shouldBe true
    }

    client.close()
  }

  it should "send requests with correct path" in {
    val requestTransformation = (r: Request) => r.method(GET)

    def partialRequest: HttpRequest =
      HttpRequest.request().withMethod(GET.asString())

    val paths = Table(
      "path",
      "/",
      "/abc/def/")

    forAll(paths) { path =>
      val client = new RestClient(hosts, path, HTTP_1_1, Map.empty[String, String])
      val expectedRequest = partialRequest.withPath(path)

      server.when(expectedRequest).respond(responseOk200)
      val isSuccess = client.execute(requestTransformation)
      server.verify(expectedRequest)
      isSuccess shouldBe true

      client.close()
    }
  }

  it should "send requests with correct headers" in {
    val headersTable = Table(
      "headers",
      Map.empty[String, String],
      Map("header1" -> "value1"),
      Map(
        "header1" -> "value1",
        "header2" -> "value2"),
      Map("header1" -> "value1"),
      Map("header1" -> "value1"))

    val path = "/some/path"

    val requestTransformation = (r: Request) => r.method(GET)

    def partialRequest: HttpRequest =
      HttpRequest.request(path).withMethod(GET.asString())

    forAll(headersTable) { headers =>
      val client = new RestClient(hosts, path, HTTP_1_1, headers)
      val expectedRequest = partialRequest
      headers.foreach(header => expectedRequest.withHeader(header._1, header._2))

      server.when(expectedRequest).respond(responseOk200)
      val isSuccess = client.execute(requestTransformation)
      server.verify(expectedRequest)
      isSuccess shouldBe true

      client.close()
    }
  }

  it should "handle different status codes properly" in {
    val path = "/some/path"
    val requestTransformation = (r: Request) => r.method(GET)
    val expectedRequest = HttpRequest.request(path).withMethod(GET.asString())
    val client = new RestClient(hosts, path, HTTP_1_1, Map.empty[String, String])

    val codesToResult = Table(
      ("codes", "expectedResult"),
      (Seq(200, 202, 215, 255, 299), true),
      (Seq(199, 300, 404, 403, 500), false))

    forAll(codesToResult) { (codes, expectedResult) =>
      codes.foreach { code =>
        server.when(expectedRequest).respond(response().withStatusCode(code))

        val isSuccess = client.execute(requestTransformation)
        server.verify(expectedRequest)
        isSuccess shouldBe expectedResult

        server.clear(expectedRequest)
      }
    }

    client.close()
  }

  it should "return false if it did not have enough time to get response" in {
    val path = "/some/path"
    val requestTransformation = (r: Request) => r.method(GET)
    val expectedRequest = HttpRequest.request(path).withMethod(GET.asString())
    val timeout: Long = 500
    val client = new RestClient(hosts, path, HTTP_1_1, Map.empty[String, String], timeout = timeout)


    server.when(HttpRequest.request()).callback { httpRequest: HttpRequest =>
      if (httpRequest.getPath.getValue == path && httpRequest.getMethod.getValue == GET.asString())
        Thread.sleep(timeout * 2)

      responseOk200
    }

    val isSuccess = client.execute(requestTransformation)
    isSuccess shouldBe false

    server.clear(expectedRequest)

    client.close()
  }

  it should "use basic authentication if username and password is defined" in {
    val path = "/some/path"
    val requestTransformation = (r: Request) => r.method(GET)

    val username = "user"
    val password = "pass"
    val encoded = BaseEncoding.base64().encode(s"$username:$password".getBytes)
    val expectedRequest = HttpRequest.request(path).withMethod(GET.asString())
      .withHeader("Authorization", "Basic " + encoded)
    server.when(expectedRequest).respond(responseOk200)

    val client = new RestClient(hosts, path, HTTP_1_1, Map.empty[String, String], Some(username), Some(password))
    val isSuccess = client.execute(requestTransformation)
    server.verify(expectedRequest)
    isSuccess shouldBe true

    client.close()
  }


  override def afterAll(): Unit = server.stop()
}
