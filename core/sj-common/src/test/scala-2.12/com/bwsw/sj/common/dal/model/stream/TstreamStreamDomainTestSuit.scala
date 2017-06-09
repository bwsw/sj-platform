package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.bwsw.tstreams.storage.StorageClient
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

class TstreamStreamDomainTestSuit extends FlatSpec with Matchers with PrivateMethodTester with TstreamStreamDomainMocks {
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
