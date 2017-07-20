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

import java.util.Date

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class KafkaStreamDomainTestSuit extends FlatSpec with Matchers with KafkaStreamDomainMocks {
  it should "create() method creates a new topic if the topic doesn't exist" in {
    //arrange
    val kafkaClient = mock[KafkaClient]
    when(kafkaClient.topicExists(name)).thenReturn(false)
    val kafkaStreamDomain = streamDomain(kafkaClient)

    //act
    kafkaStreamDomain.create()

    //assert
    verify(kafkaClient, times(1)).createTopic(name, partitions, replicationFactor)
    verify(kafkaClient, times(1)).close()
  }

  it should "create() method doesn't create a topic if the topic already exists" in {
    //arrange
    val kafkaClient = mock[KafkaClient]
    when(kafkaClient.topicExists(name)).thenReturn(true)
    val kafkaStreamDomain = streamDomain(kafkaClient)

    //act
    kafkaStreamDomain.create()

    //assert
    verify(kafkaClient, never()).createTopic(name, partitions, replicationFactor)
    verify(kafkaClient, times(1)).close()
  }

  it should "create() method throws an exception if there is any exception during the creation process" in {
    //arrange
    val exception = new RuntimeException()
    val kafkaClient = mock[KafkaClient]
    when(kafkaClient.topicExists(name)).thenThrow(exception)
    val kafkaStreamDomain = streamDomain(kafkaClient)

    //act and assert
    assertThrows[RuntimeException](kafkaStreamDomain.create())
  }
}

trait KafkaStreamDomainMocks extends MockitoSugar {
  val name = "kafka-stream"
  val partitions = 1
  val replicationFactor = 1
  private val provider = mock[ProviderDomain]
  when(provider.hosts).thenReturn(Array[String]())
  private val service = mock[KafkaServiceDomain]
  when(service.zkProvider).thenReturn(provider)

  def streamDomain(kafkaClient: KafkaClient) =
    new KafkaStreamDomain(name, service, partitions, replicationFactor, creationDate = new Date()) {
      override def createClient(): KafkaClient = {
        kafkaClient
      }
    }
}