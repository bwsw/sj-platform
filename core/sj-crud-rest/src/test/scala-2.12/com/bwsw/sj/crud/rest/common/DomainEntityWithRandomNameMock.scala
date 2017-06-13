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
package com.bwsw.sj.crud.rest.common

import java.util.UUID

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar

class InstanceDomainWithRandomNameMock() extends MockitoSugar {
  val instanceDomain: InstanceDomain = mock[InstanceDomain]
  when(instanceDomain.name).thenReturn(UUID.randomUUID().toString)
}

class ServiceDomainWithRandomNameMock() extends MockitoSugar {
  val serviceDomain: ServiceDomain = mock[ServiceDomain]
  when(serviceDomain.name).thenReturn(UUID.randomUUID().toString)
}

class StreamDomainWithRandomNameMock() extends MockitoSugar {
  val streamDomain: StreamDomain = mock[StreamDomain]
  when(streamDomain.name).thenReturn(UUID.randomUUID().toString)
  private val serviceDomain = new ServiceDomainWithRandomNameMock().serviceDomain
  when(streamDomain.service).thenReturn(serviceDomain)
}

class ProviderDomainWithRandomNameMock() extends MockitoSugar {
  val providerDomain: ProviderDomain = mock[ProviderDomain]
  when(providerDomain.name).thenReturn(UUID.randomUUID().toString)
}