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
package com.bwsw.sj.crud.rest.instance.validator

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.common._
import org.scalatest.mockito.MockitoSugar
import scaldi.{Injector, Module}
import org.mockito.Mockito.when

trait InstanceValidatorMocks extends MockitoSugar {
  private val instanceRepositoryMock = new InstanceRepositoryMock()
  private val serviceRepositoryMock = new ServiceRepositoryMock()
  private val streamRepositoryMock = new StreamRepositoryMock()
  private val providerRepositoryMock = new ProviderRepositoryMock()

  private val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getInstanceRepository).thenReturn(instanceRepositoryMock.repository)
  when(connectionRepository.getServiceRepository).thenReturn(serviceRepositoryMock.repository)
  when(connectionRepository.getStreamRepository).thenReturn(streamRepositoryMock.repository)
  when(connectionRepository.getProviderRepository).thenReturn(providerRepositoryMock.repository)

  private val module = new Module {
    bind[ConnectionRepository] to connectionRepository
    bind[MessageResourceUtils] to MessageResourceUtilsMock.messageResourceUtils
  }
  protected val injector: Injector = module.injector

  def getStreamStorage: GenericMongoRepository[StreamDomain] = streamRepositoryMock.repository

  def getServiceStorage: GenericMongoRepository[ServiceDomain] = serviceRepositoryMock.repository
}
