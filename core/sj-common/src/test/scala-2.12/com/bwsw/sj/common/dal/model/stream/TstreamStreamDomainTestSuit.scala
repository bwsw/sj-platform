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
package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.bwsw.tstreams.storage.StorageClient
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class TstreamStreamDomainTestSuit extends FlatSpec with Matchers with TstreamStreamDomainMocks {
  it should "create() method creates a new topic if the topic doesn't exist" in {
    //arrange
    val storageClient = mock[StorageClient]
    when(storageClient.checkStreamExists(name)).thenReturn(false)
    val tStreamStreamDomain = streamDomain(storageClient)

    //act
    tStreamStreamDomain.create()

    //assert
    verify(storageClient, times(1)).createStream(name, partitions, StreamLiterals.ttl, description)
    verify(storageClient, times(1)).shutdown()
  }

  it should "create() method doesn't create a topic if the topic already exists" in {
    //arrange
    val storageClient = mock[StorageClient]
    when(storageClient.checkStreamExists(name)).thenReturn(true)
    val tStreamStreamDomain = streamDomain(storageClient)

    //act
    tStreamStreamDomain.create()

    //assert
    verify(storageClient, never()).createStream(name, partitions, StreamLiterals.ttl, description)
    verify(storageClient, times(1)).shutdown()
  }

  it should "create() method throws an exception if there is any exception during the creation process" in {
    //arrange
    val exception = new RuntimeException()
    val storageClient = mock[StorageClient]
    when(storageClient.checkStreamExists(name)).thenThrow(exception)
    val tStreamStreamDomain = streamDomain(storageClient)

    //act and assert
    assertThrows[RuntimeException](tStreamStreamDomain.create())
  }
}

trait TstreamStreamDomainMocks extends MockitoSugar {
  val name = "tstream-stream"
  val partitions = 1
  val description: String = RestLiterals.defaultDescription
  private val provider = mock[ProviderDomain]
  when(provider.hosts).thenReturn(Array[String]())
  private val service = mock[TStreamServiceDomain]
  when(service.provider).thenReturn(provider)

  def streamDomain(storageClient: StorageClient) =
    new TStreamStreamDomain(name, service, partitions, description) {
      override def createClient(): StorageClient = {
        storageClient
      }
    }
}
