package com.bwsw.sj.common.si

import java.util.UUID

import com.bwsw.sj.common.dal.model.instance._
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.stream.{SjStream, CreateStream}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.{MessageResourceUtils, MessageResourceUtilsMock}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{never, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class StreamSiTests extends FlatSpec with Matchers {

  "StreamSI" should "create correct stream" in new StreamMocks {
    when(stream.validate()).thenReturn(ArrayBuffer[String]())

    streamSI.create(stream) shouldBe Created
    verify(stream).create()
    streamStorage.toSet shouldBe (initStreamStorage + streamDomain)
  }

  it should "not create incorrect stream" in new StreamMocks {
    val errors = ArrayBuffer("Not valid")
    when(stream.validate()).thenReturn(errors)

    streamSI.create(stream) shouldBe NotCreated(errors)
    verify(stream, never).create()
    streamStorage.toSet shouldBe initStreamStorage
  }

  it should "give all streams" in new StreamMocks {
    streamSI.getAll().toSet shouldBe streams.toSet
  }

  it should "give stream when it exists" in new StreamMocks {
    streamStorage += streamDomain
    streams += stream

    streamSI.get(streamName) shouldBe Some(stream)
  }

  it should "not give stream when it does not exists" in new StreamMocks {
    streamSI.get(nonExistsStreamName) shouldBe empty
  }

  it should "give empty arrays when stream does not have related instances" in new StreamMocksWithRelated {
    streamSI.getRelated(streamWithoutRelatedName) shouldBe Some(mutable.Buffer.empty[String])
  }

  it should "give related instances when stream has them" in new StreamMocksWithRelated {
    val related = streamSI.getRelated(streamWithRelatedName)
    related.map(_.toSet) shouldBe Some(instanceRelatedNames.toSet)
  }

  it should "tell that stream does not exists in getRelated()" in new StreamMocksWithRelated {
    streamSI.getRelated(nonExistsStreamName) shouldBe None
  }

  it should "delete stream when it does not have related instances" in new StreamMocksWithRelated {
    streamSI.delete(streamWithoutRelatedName) shouldBe Deleted
    verify(streamWithoutRelated).delete()
    streamStorage.toSet shouldBe (initStreamStorage - streamWithoutRelatedDomain)
  }

  it should "not delete stream when it does not exists" in new StreamMocksWithRelated {
    streamSI.delete(nonExistsStreamName) shouldBe EntityNotFound
    streamStorage.toSet shouldBe initStreamStorage
  }

  it should "not delete stream when it have related instances" in new StreamMocksWithRelated {
    val deletionError = s"rest.streams.stream.cannot.delete:$streamWithRelatedName"

    streamSI.delete(streamWithRelatedName) shouldBe DeletionError(deletionError)
    verify(streamWithRelated, never).delete()
    streamStorage.toSet shouldBe initStreamStorage
  }

  trait StreamMocks extends MockitoSugar {
    val nonExistsStreamName = "non-exist-stream"

    val streamName = "stream-name"
    val streamDomain = mock[StreamDomain]
    when(streamDomain.name).thenReturn(streamName)
    val stream = mock[SjStream]
    when(stream.name).thenReturn(streamName)
    when(stream.to()).thenReturn(streamDomain)

    val initStreamStorageSize = 10
    val streamStorage: mutable.Buffer[StreamDomain] = Range(0, initStreamStorageSize).map { _ =>
      val streamDomain = mock[StreamDomain]
      when(streamDomain.name).thenReturn(UUID.randomUUID().toString)
      streamDomain
    }.toBuffer
    val initStreamStorage: Set[StreamDomain] = streamStorage.toSet
    val streams = streamStorage.map { streamDomain =>
      val stream = mock[SjStream]
      val streamName = streamDomain.name
      when(stream.name).thenReturn(streamName)
      stream
    }

    val streamRepository = mock[GenericMongoRepository[StreamDomain]]
    when(streamRepository.getAll).thenReturn({
      streamStorage
    })
    when(streamRepository.save(any[StreamDomain]()))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        streamStorage += invocationOnMock.getArgument[StreamDomain](0)
      })
    when(streamRepository.delete(anyString()))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val streamName = invocationOnMock.getArgument[String](0)
        streamStorage -= streamStorage.find(_.name == streamName).get
      })
    when(streamRepository.get(anyString()))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val streamName = invocationOnMock.getArgument[String](0)
        streamStorage.find(_.name == streamName)
      })

    val connectionRepository = mock[ConnectionRepository]
    when(connectionRepository.getStreamRepository).thenReturn(streamRepository)

    val createStream = mock[CreateStream]
    when(createStream.from(any[StreamDomain])(any[Injector]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val streamDomain = invocationOnMock.getArgument[StreamDomain](0)
        streams.find(_.name == streamDomain.name).get
      })

    val module = new Module {
      bind[ConnectionRepository] to connectionRepository
      bind[MessageResourceUtils] to MessageResourceUtilsMock.messageResourceUtils
      bind[CreateStream] to createStream
    }
    val injector = module.injector
    val streamSI = new StreamSI()(injector)
  }

  trait StreamMocksWithRelated extends StreamMocks {
    val streamWithoutRelatedName = "stream-without-related"
    val streamWithoutRelatedDomain = mock[StreamDomain]
    when(streamWithoutRelatedDomain.name).thenReturn(streamWithoutRelatedName)
    val streamWithoutRelated = mock[SjStream]
    when(streamWithoutRelated.name).thenReturn(streamWithoutRelatedName)
    when(streamWithoutRelated.to()).thenReturn(streamWithoutRelatedDomain)

    val streamWithRelatedName = "stream-with-related"
    val streamWithRelatedDomain = mock[StreamDomain]
    when(streamWithRelatedDomain.name).thenReturn(streamWithRelatedName)
    val streamWithRelated = mock[SjStream]
    when(streamWithRelated.name).thenReturn(streamWithRelatedName)
    when(streamWithRelated.to()).thenReturn(streamWithRelatedDomain)

    streamStorage ++= mutable.Buffer(streamWithRelatedDomain, streamWithoutRelatedDomain)
    streams ++= mutable.Buffer(streamWithRelated, streamWithoutRelated)

    override val initStreamStorage: Set[StreamDomain] = streamStorage.toSet

    val instancesRelatedInInputs = Seq(
      mock[BatchInstanceDomain],
      mock[RegularInstanceDomain],
      mock[OutputInstanceDomain])

    instancesRelatedInInputs.foreach { instance =>
      when(instance.name).thenReturn(UUID.randomUUID().toString)
      when(instance.getInputsWithoutStreamMode).thenReturn(Array(streamWithRelatedName))
      when(instance.outputs).thenReturn(Array("other-stream"))
    }

    val instancesRelatedInOutputs = Seq(
      mock[InputInstanceDomain],
      mock[BatchInstanceDomain],
      mock[RegularInstanceDomain],
      mock[OutputInstanceDomain])

    instancesRelatedInOutputs.foreach { instance =>
      when(instance.name).thenReturn(UUID.randomUUID().toString)
      when(instance.getInputsWithoutStreamMode).thenReturn(Array("other-stream"))
      when(instance.outputs).thenReturn(Array(streamWithRelatedName))
    }

    val instancesRelatedInBoth = Seq(
      mock[BatchInstanceDomain],
      mock[RegularInstanceDomain],
      mock[OutputInstanceDomain])

    instancesRelatedInBoth.foreach { instance =>
      when(instance.name).thenReturn(UUID.randomUUID().toString)
      when(instance.getInputsWithoutStreamMode).thenReturn(Array(streamWithRelatedName))
      when(instance.outputs).thenReturn(Array(streamWithRelatedName))
    }

    val instancesRelated = instancesRelatedInInputs ++ instancesRelatedInOutputs ++ instancesRelatedInBoth
    instancesRelated.foreach(instance => when(instance.name).thenReturn(UUID.randomUUID().toString))

    val instanceRelatedNames = instancesRelated.map(_.name)


    val notRelatedInstances = Seq(
      mock[InputInstanceDomain],
      mock[BatchInstanceDomain],
      mock[RegularInstanceDomain],
      mock[OutputInstanceDomain])

    notRelatedInstances.foreach { instance =>
      when(instance.name).thenReturn(UUID.randomUUID().toString)
      when(instance.getInputsWithoutStreamMode).thenReturn(Array("other-stream"))
      when(instance.outputs).thenReturn(Array("other-stream"))
    }

    val allInstances = (instancesRelated ++ notRelatedInstances).toBuffer
    val instanceRepository = mock[GenericMongoRepository[InstanceDomain]]
    when(instanceRepository.getAll).thenReturn(allInstances)
    when(connectionRepository.getInstanceRepository).thenReturn(instanceRepository)

    override val streamSI = new StreamSI()(injector)
  }

}
