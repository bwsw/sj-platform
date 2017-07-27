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
package com.bwsw.common.marathon

import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.charset.StandardCharsets

import com.bwsw.common.JsonSerializer
import com.bwsw.common.http.HttpClient
import com.bwsw.sj.common.utils.FrameworkLiterals
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost, HttpPut, HttpUriRequest}
import org.apache.http.{HttpEntity, HttpStatus, StatusLine}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class MarathonApiTestSuit extends FlatSpec with Matchers {

  it should s"getApplicationEntity() method works properly, " +
    s"i.e extract marathon application info from http entity and deserialize to ${classOf[MarathonApplication]}" in new MarathonApiMocks {
    //arrange
    val response = getClosableHttpResponseWithEntityMock(marathonApplicationStub)

    //act
    val marathonApplication = marathonApi.getApplicationEntity(response)

    //assert
    marathonApplication shouldBe marathonApplicationStub
  }

  it should s"getMarathonMaster() method works properly" in new MarathonApiMocks {
    //arrange
    val response = getClosableHttpResponseWithEntityMock(marathonInfoStub)

    //act
    val marathonMaster = marathonApi.getMarathonMaster(response)

    //assert
    marathonMaster shouldBe marathonConfigStub.master
  }

  it should s"getLeaderTask() method returns marathon task if marathon info contains it" in new MarathonApiMocks {
    //arrange
    val response = getClosableHttpResponseWithEntityMock(marathonApplicationStub)

    //act
    val leaderTask = marathonApi.getLeaderTask(response)

    //assert
    leaderTask.get shouldBe marathonTasksStub
  }

  it should s"getLeaderTask() method returns None if marathon info doesn't contain it" in new MarathonApiMocks {
    //arrange
    val response = getClosableHttpResponseWithEntityMock(emptyMarathonApplication)

    //act
    val leaderTask = marathonApi.getLeaderTask(response)

    //assert
    leaderTask shouldBe None
  }

  it should s"getFrameworkID() method returns framework id if marathon info contains it" in new MarathonApiMocks {
    //arrange
    val response = getClosableHttpResponseWithEntityMock(marathonApplicationStub)

    //act
    val frameworkId = marathonApi.getFrameworkID(response)

    //assert
    frameworkId.get shouldBe frameworkIdStub
  }

  it should s"getFrameworkID() method returns None if marathon info doesn't contain it" in new MarathonApiMocks {
    //arrange
    val response = getClosableHttpResponseWithEntityMock(emptyMarathonApplication)

    //act
    val frameworkId = marathonApi.getFrameworkID(response)

    //assert
    frameworkId shouldBe None
  }

  it should s"getMarathonInfo() method works properly" in new MarathonApiMocks {
    //arrange
    val expectedUri = new URI(s"$marathonAddress/v2/info")
    var uri: URI = _
    val marathonInfo = getClosableHttpResponseWithEntityMock(marathonInfoStub)
    when(clientMock.execute(any[HttpUriRequest]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val httpUriRequest = invocationOnMock.getArgument[HttpUriRequest](0)

        uri = httpUriRequest.getURI

        marathonInfo
      })

    //act
    val response = marathonApi.getMarathonInfo()

    //assert
    response shouldBe marathonInfo
    uri shouldBe expectedUri
  }


  it should s"startMarathonApplication() method returns a correct status code. " +
    s"Also the method should call certain marathon API method and use passed content for creation of the request" in new MarathonApiMocks {
    //arrange
    val expectedStatusCode = HttpStatus.SC_CREATED
    val expectedUri = new URI(s"$marathonAddress/v2/apps")
    var uri: URI = _
    var entity: HttpEntity = _
    val marathonInfo = getClosableHttpResponseMock(marathonRequestStub, expectedStatusCode)
    when(clientMock.execute(any[HttpUriRequest]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val httpUriRequest = invocationOnMock.getArgument[HttpUriRequest](0)

        uri = httpUriRequest.getURI
        entity = httpUriRequest.asInstanceOf[HttpPost].getEntity

        marathonInfo
      })

    //act
    val statusCode = marathonApi.startMarathonApplication(marathonRequestStub)

    //assert
    statusCode shouldBe expectedStatusCode
    uri shouldBe expectedUri
    IOUtils.contentEquals(entity.getContent, marathonInfo.getEntity.getContent) shouldBe true
  }

  it should s"getApplicationInfo() method works properly" in new MarathonApiMocks {
    //arrange
    val appId = "app_id"
    val expectedUri = new URI(s"$marathonAddress/v2/apps/$appId?force=true")
    var uri: URI = _
    val marathonInfo = getClosableHttpResponseWithEntityMock(marathonApplicationStub)
    when(clientMock.execute(any[HttpUriRequest]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val httpUriRequest = invocationOnMock.getArgument[HttpUriRequest](0)

        uri = httpUriRequest.getURI

        marathonInfo
      })

    //act
    val response = marathonApi.getApplicationInfo(appId)

    //assert
    response shouldBe marathonInfo
    uri shouldBe expectedUri
  }

  it should s"getMarathonLastTaskFailed() method works properly" in new MarathonApiMocks {
    //arrange
    val appId = "app_id"
    val expectedUri = new URI(s"$marathonAddress/v2/apps/$appId?embed=lastTaskFailure")
    var uri: URI = _
    val marathonInfo = getClosableHttpResponseWithEntityMock(marathonApplicationStub)
    when(clientMock.execute(any[HttpUriRequest]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val httpUriRequest = invocationOnMock.getArgument[HttpUriRequest](0)

        uri = httpUriRequest.getURI

        marathonInfo
      })

    //act
    val response = marathonApi.getMarathonLastTaskFailed(appId)

    //assert
    response shouldBe marathonInfo
    uri shouldBe expectedUri
  }

  it should s"destroyMarathonApplication() method returns a correct status code " +
    s"and calls certain marathon API method using passed application id" in new MarathonApiMocks {
    //arrange
    val appId = "app_id"
    val expectedStatusCode = HttpStatus.SC_CREATED
    val expectedUri = new URI(s"$marathonAddress/v2/apps/$appId")
    var uri: URI = _
    val marathonInfo = getClosableHttpResponseWithStatusMock(expectedStatusCode)
    when(clientMock.execute(any[HttpUriRequest]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val httpUriRequest = invocationOnMock.getArgument[HttpUriRequest](0)

        uri = httpUriRequest.getURI

        marathonInfo
      })

    //act
    val statusCode = marathonApi.destroyMarathonApplication(appId)

    //assert
    statusCode shouldBe expectedStatusCode
    uri shouldBe expectedUri
  }

  it should s"stopMarathonApplication() method returns a correct status code " +
    s"and calls certain marathon API method using passed application id" in new MarathonApiMocks {
    //arrange
    val appId = "app_id"
    val expectedStatusCode = HttpStatus.SC_OK
    val expectedUri = new URI(s"$marathonAddress/v2/apps/$appId?force=true")
    var uri: URI = _
    var entity: HttpEntity = _
    val marathonInfo = getClosableHttpResponseMock(MarathonApplicationInstances(0), expectedStatusCode)
    when(clientMock.execute(any[HttpUriRequest]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val httpUriRequest = invocationOnMock.getArgument[HttpUriRequest](0)

        uri = httpUriRequest.getURI
        entity = httpUriRequest.asInstanceOf[HttpPut].getEntity

        marathonInfo
      })

    //act
    val statusCode = marathonApi.stopMarathonApplication(appId)

    //assert
    statusCode shouldBe expectedStatusCode
    uri shouldBe expectedUri
    IOUtils.contentEquals(entity.getContent, marathonInfo.getEntity.getContent) shouldBe true
  }

  it should s"scaleMarathonApplication() method returns a correct status code " +
    s"and calls certain marathon API method using passed parameters: application id and count of instances" in new MarathonApiMocks {
    //arrange
    val appId = "app_id"
    val countOfInstances = 10
    val expectedStatusCode = HttpStatus.SC_OK
    val expectedUri = new URI(s"$marathonAddress/v2/apps/$appId?force=true")
    var uri: URI = _
    var entity: HttpEntity = _
    val marathonInfo = getClosableHttpResponseMock(MarathonApplicationInstances(countOfInstances), expectedStatusCode)
    when(clientMock.execute(any[HttpUriRequest]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val httpUriRequest = invocationOnMock.getArgument[HttpUriRequest](0)

        uri = httpUriRequest.getURI
        entity = httpUriRequest.asInstanceOf[HttpPut].getEntity

        marathonInfo
      })

    //act
    val statusCode = marathonApi.scaleMarathonApplication(appId, countOfInstances)

    //assert
    statusCode shouldBe expectedStatusCode
    uri shouldBe expectedUri
    IOUtils.contentEquals(entity.getContent, marathonInfo.getEntity.getContent) shouldBe true
  }
}

/**
  * ref. [[https://mesosphere.github.io/marathon/docs/rest-api.html]]
  */
trait MarathonApiMocks extends MockitoSugar {
  val marathonAddress = "http://host:8080"
  val clientMock: HttpClient = mock[HttpClient]

  val marathonApi = new MarathonApi(clientMock, marathonAddress)
  val serializer = new JsonSerializer(true)

  def getClosableHttpResponseWithEntityMock(content: Serializable): CloseableHttpResponse = {
    val entity = getHttpEntityMock(content)

    val responseMock = mock[CloseableHttpResponse]
    when(responseMock.getEntity).thenReturn(entity)

    responseMock
  }

  def getClosableHttpResponseWithStatusMock(status: Int): CloseableHttpResponse = {
    val statusLineMock = mock[StatusLine]
    when(statusLineMock.getStatusCode).thenReturn(status)

    val responseMock = mock[CloseableHttpResponse]
    when(responseMock.getStatusLine).thenReturn(statusLineMock)

    responseMock
  }

  def getClosableHttpResponseMock(content: Serializable, status: Int): CloseableHttpResponse = {
    val statusLineMock = mock[StatusLine]
    when(statusLineMock.getStatusCode).thenReturn(status)

    val entity = getHttpEntityMock(content)

    val responseMock = mock[CloseableHttpResponse]
    when(responseMock.getStatusLine).thenReturn(statusLineMock)
    when(responseMock.getEntity).thenReturn(entity)

    responseMock
  }

  private def getHttpEntityMock(content: Serializable): HttpEntity = {
    val serializedContent = new ByteArrayInputStream(serializer.serialize(content).getBytes(StandardCharsets.UTF_8))

    val entityMock = mock[HttpEntity]
    when(entityMock.getContent).thenReturn(serializedContent)

    entityMock
  }

  private val marathonTaskFailureStub = MarathonTaskFailure("127.0.0.1", "Abnormal executor termination", "TASK_FAILED", "2014-09-12T23:23:41.711Z")

  val marathonTasksStub = MarathonTask("id", "127.0.0.1", List(31045))
  val frameworkIdStub = "framework_id"
  private val marathonApplicationInfoStub = MarathonApplicationInfo("id", Map(FrameworkLiterals.frameworkIdLabel -> frameworkIdStub), 1, List(marathonTasksStub), marathonTaskFailureStub)
  val marathonApplicationStub = MarathonApplication(marathonApplicationInfoStub)

  private val emptyMarathonApplicationInfo = MarathonApplicationInfo("id", Map(), 1, List(), marathonTaskFailureStub)
  val emptyMarathonApplication = MarathonApplication(emptyMarathonApplicationInfo)

  val marathonConfigStub = MarathonConfig("zk://localhost:2181/mesos")
  val zooKeeperConfigStub = ZooKeeperConfig("zk://localhost:2181/marathon")
  val marathonInfoStub = MarathonInfo(marathonConfigStub, zooKeeperConfigStub)

  val marathonRequestStub = MarathonRequest("id", "sleep 100", 1, Map(), List())
}