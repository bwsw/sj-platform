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