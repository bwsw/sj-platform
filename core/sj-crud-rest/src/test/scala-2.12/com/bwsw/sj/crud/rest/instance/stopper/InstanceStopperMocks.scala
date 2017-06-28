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
package com.bwsw.sj.crud.rest.instance.stopper

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.bwsw.common.JsonSerializer
import com.bwsw.common.marathon._
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.common.InstanceRepositoryMock
import com.bwsw.sj.crud.rest.instance._
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.{HttpEntity, HttpStatus, StatusLine}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import scaldi.{Injector, Module}

trait InstanceStopperMocks extends MockitoSugar {
  private val marathonAddress = "http://host:8080"
  private val instanceRepositoryMock = new InstanceRepositoryMock()

  private val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getInstanceRepository).thenReturn(instanceRepositoryMock.repository)

  private val module = new Module {
    bind[ConnectionRepository] to connectionRepository
  }
  protected val injector: Injector = module.injector

  val instanceName = "instance-name"
  val frameworkId = "framework-id"
  val instanceMock: Instance = mock[Instance]
  when(instanceMock.name).thenReturn(instanceName)
  when(instanceMock.frameworkId).thenReturn(frameworkId)
  when(instanceMock.moduleType).thenReturn(EngineLiterals.regularStreamingType)

  private val serializer = new JsonSerializer(true)

  private val marathonApplicationInfoStub = MarathonApplicationInfo("id", Map(), 1, List(), null)
  private val notStartedMarathonApplicationInfoStub = MarathonApplicationInfo("id", Map(), 0, List(), null)
  val marathonApplicationStub = MarathonApplication(marathonApplicationInfoStub)
  val notStartedMarathonApplicationStub = MarathonApplication(notStartedMarathonApplicationInfoStub)

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

  def instanceStopper(instanceMock: Instance = instanceMock) = new InstanceStopper(instanceMock, marathonAddress)(injector)

  def instanceStopperMock(marathonManager: MarathonApi = mock[MarathonApi],
                          instanceManager: InstanceDomainRenewer = mock[InstanceDomainRenewer],
                          instanceMock: Instance = instanceMock): InstanceStopperMock = {
    new InstanceStopperMock(marathonManager, instanceManager, instanceMock, marathonAddress)(injector)
  }

  val frameworkName: String = InstanceAdditionalFieldCreator.getFrameworkName(instanceMock)

  val okStatus = HttpStatus.SC_OK
  val errorStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
}
