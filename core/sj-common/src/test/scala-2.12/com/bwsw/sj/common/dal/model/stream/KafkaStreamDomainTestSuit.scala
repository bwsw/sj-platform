package com.bwsw.sj.common.dal.model.stream

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
    new KafkaStreamDomain(name, service, partitions, replicationFactor) {
      override def createClient(): KafkaClient = {
        kafkaClient
      }
    }
}