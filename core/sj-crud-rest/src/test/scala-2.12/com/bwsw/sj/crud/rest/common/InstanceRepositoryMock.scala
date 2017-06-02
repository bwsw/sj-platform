package com.bwsw.sj.crud.rest.common

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar

import scala.collection.mutable

class InstanceRepositoryMock() extends MockitoSugar {
  private val initStorageSize = 10
  private val storage: mutable.Buffer[InstanceDomain] = Range(0, initStorageSize).map { _ =>
    new InstanceDomainWithRandomNameMock().instanceDomain
  }.toBuffer

  val repository: GenericMongoRepository[InstanceDomain] = mock[GenericMongoRepository[InstanceDomain]]

  when(repository.get(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val instanceName = invocationOnMock.getArgument[String](0)
      storage.find(_.name == instanceName)
    })

  when(repository.save(any[InstanceDomain]()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val instanceDomain = invocationOnMock.getArgument[InstanceDomain](0)
      storage += instanceDomain
    })

  when(repository.delete(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val name = invocationOnMock.getArgument[String](0)
      val instanceDomain = storage.find(_.name == name).get
      storage -= instanceDomain
    })

  when(repository.getAll).thenReturn({
    storage
  })
}
