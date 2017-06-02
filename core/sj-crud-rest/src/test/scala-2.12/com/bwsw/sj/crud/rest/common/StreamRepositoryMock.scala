package com.bwsw.sj.crud.rest.common

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar

import scala.collection.mutable

class StreamRepositoryMock() extends MockitoSugar {
  private val initStorageSize = 10
  private val storage: mutable.Buffer[StreamDomain] = Range(0, initStorageSize).map { _ =>
    val stream = new StreamDomainWithRandomNameMock().streamDomain

    stream
  }.toBuffer

  val repository: GenericMongoRepository[StreamDomain] = mock[GenericMongoRepository[StreamDomain]]

  when(repository.getAll).thenReturn({
    storage
  })

  when(repository.save(any[StreamDomain]()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val streamDomain = invocationOnMock.getArgument[StreamDomain](0)
      storage += streamDomain
    })

  when(repository.get(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val name = invocationOnMock.getArgument[String](0)
      storage.find(_.name == name)
    })
}