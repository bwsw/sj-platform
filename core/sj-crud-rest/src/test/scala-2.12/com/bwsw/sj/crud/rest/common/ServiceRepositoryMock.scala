package com.bwsw.sj.crud.rest.common

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar

import scala.collection.mutable

class ServiceRepositoryMock() extends MockitoSugar {
  private val initStorageSize = 10
  private val storage: mutable.Buffer[ServiceDomain] = Range(0, initStorageSize).map { _ =>
    new ServiceDomainWithRandomNameMock().serviceDomain
  }.toBuffer

  val repository: GenericMongoRepository[ServiceDomain] = mock[GenericMongoRepository[ServiceDomain]]

  when(repository.save(any[ServiceDomain]()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val serviceDomain = invocationOnMock.getArgument[ServiceDomain](0)
      storage += serviceDomain
    })

  when(repository.get(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val name = invocationOnMock.getArgument[String](0)
      storage.find(_.name == name)
    })
}

