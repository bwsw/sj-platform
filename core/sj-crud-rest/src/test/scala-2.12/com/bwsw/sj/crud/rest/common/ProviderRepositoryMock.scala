package com.bwsw.sj.crud.rest.common

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar

import scala.collection.mutable

class ProviderRepositoryMock extends MockitoSugar {
  private val initStorageSize = 10
  private val storage: mutable.Buffer[ProviderDomain] = Range(0, initStorageSize).map { _ =>
    new ProviderDomainWithRandomNameMock().providerDomain
  }.toBuffer


  val repository: GenericMongoRepository[ProviderDomain] = mock[GenericMongoRepository[ProviderDomain]]

  when(repository.get(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val name = invocationOnMock.getArgument[String](0)
      storage.find(_.name == name)
    })

}
