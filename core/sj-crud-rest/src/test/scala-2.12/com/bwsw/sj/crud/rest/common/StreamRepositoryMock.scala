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