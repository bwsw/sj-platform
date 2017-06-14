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
package com.bwsw.sj.crud.rest.instance.destroyer

import com.bwsw.common.marathon._
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.common.InstanceRepositoryMock
import com.bwsw.sj.crud.rest.instance._
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.{HttpStatus, StatusLine}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import scaldi.{Injector, Module}

trait InstanceDestroyerMocks extends MockitoSugar {
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

  private def getClosableHttpResponseMock(status: Int): CloseableHttpResponse = {
    val statusLineMock = mock[StatusLine]
    when(statusLineMock.getStatusCode).thenReturn(status)

    val responseMock = mock[CloseableHttpResponse]
    when(responseMock.getStatusLine).thenReturn(statusLineMock)

    responseMock
  }

  def instanceDestroyerMock(marathonManager: MarathonApi = mock[MarathonApi],
                            instanceManager: InstanceDomainRenewer = mock[InstanceDomainRenewer],
                            instanceMock: Instance = instanceMock): InstanceDestroyerMock = {
    new InstanceDestroyerMock(marathonManager, instanceManager, instanceMock)(injector)
  }

  val frameworkName: String = InstanceAdditionalFieldCreator.getFrameworkName(instanceMock)

  val okStatus = HttpStatus.SC_OK
  val errorStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
  val okFrameworkResponse: CloseableHttpResponse = getClosableHttpResponseMock(okStatus)
  val notFoundFrameworkResponce: CloseableHttpResponse = getClosableHttpResponseMock(HttpStatus.SC_NOT_FOUND)
}
