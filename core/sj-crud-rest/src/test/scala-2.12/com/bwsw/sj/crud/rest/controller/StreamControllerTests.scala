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
package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.JsonSerializer
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.StreamSI
import com.bwsw.sj.common.si.model.stream.SjStream
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.common.utils.StreamLiterals._
import com.bwsw.sj.crud.rest.{RelatedToStreamResponseEntity, _}
import com.bwsw.sj.crud.rest.model.stream.{StreamApi, StreamApiCreator}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class StreamControllerTests extends FlatSpec with Matchers with MockitoSugar {
  val entityDeletedMessageName = "rest.streams.stream.deleted"
  val entityNotFoundMessageName = "rest.streams.stream.notfound"
  val createdMessageName = "rest.streams.stream.created"
  val cannotCreateMessageName = "rest.streams.stream.cannot.create"

  val creationError = ArrayBuffer("not created")

  val serializer = mock[JsonSerializer]

  val messageResourceUtils = mock[MessageResourceUtils]
  val createStreamApi = mock[StreamApiCreator]

  val serviceInterface = mock[StreamSI]
  when(serviceInterface.get(anyString())).thenReturn(None)
  when(serviceInterface.delete(anyString())).thenReturn(EntityNotFound)
  when(serviceInterface.create(any[SjStream]())).thenReturn(NotCreated(creationError))
  when(serviceInterface.getRelated(anyString())).thenReturn(None)

  val jsonDeserializationErrorMessageCreator = mock[JsonDeserializationErrorMessageCreator]

  val streamsCount = 3
  val allStreams = Range(0, streamsCount).map(i => createStream(s"name$i"))
  allStreams.foreach {
    case StreamInfo(_, _, stream, name) =>
      when(serviceInterface.get(name)).thenReturn(Some(stream))
      when(serviceInterface.delete(name)).thenReturn(Deleted)
      when(serviceInterface.getRelated(name)).thenReturn(Some(mutable.Buffer.empty[String]))
  }
  when(serviceInterface.getAll()).thenReturn(allStreams.map(_.stream).toBuffer)

  val withRelatedStreamName = "with-related-stream"
  val relatedInstances = mutable.Buffer("instance1, instance2, instance3")
  when(serviceInterface.getRelated(withRelatedStreamName)).thenReturn(Some(relatedInstances))

  val entityDeletedMessages = allStreams.map {
    case StreamInfo(_, _, _, name) =>
      val message = entityDeletedMessageName + "," + name
      when(messageResourceUtils.createMessage(entityDeletedMessageName, name)).thenReturn(message)

      (name, message)
  }

  val newStreamName = "new-stream-name"
  val newStream = createStream(newStreamName)
  when(serviceInterface.create(newStream.stream)).thenReturn(Created)

  val createdMessage = createdMessageName + "," + newStreamName
  when(messageResourceUtils.createMessage(createdMessageName, newStreamName)).thenReturn(createdMessage)

  val entityNotFoundMessage = entityNotFoundMessageName + "," + newStreamName
  val entityNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
  when(messageResourceUtils.createMessage(entityNotFoundMessageName, newStreamName))
    .thenReturn(entityNotFoundMessage)

  val notValidStreamName = "not-valid-stream-name"
  val notValidStream = createStream(notValidStreamName)

  val notValidMessage = cannotCreateMessageName + "," + notValidStreamName
  when(messageResourceUtils.createMessageWithErrors(cannotCreateMessageName, creationError)).thenReturn(notValidMessage)

  val notDeletedStreamName = "not-deleted-stream-name"
  val notDeletedStream = createStream(notDeletedStreamName)
  val deletionError = "stream-not-deleted"
  when(serviceInterface.delete(notDeletedStreamName)).thenReturn(DeletionError(deletionError))

  val incorrectJson = "{not a json}"
  val incorrectJsonException = new JsonDeserializationException("json is incorrect")
  val incorrectJsonError = "error: json is incorrect"
  when(serializer.deserialize[StreamApi](incorrectJson)).thenAnswer(_ => throw incorrectJsonException)
  when(jsonDeserializationErrorMessageCreator.apply(incorrectJsonException)).thenReturn(incorrectJsonError)

  val incorrectJsonMessage = cannotCreateMessageName + "," + incorrectJson
  when(messageResourceUtils.createMessage(cannotCreateMessageName, incorrectJsonError)).thenReturn(incorrectJsonMessage)

  val injector = new Module {
    bind[JsonSerializer] to serializer
    bind[MessageResourceUtils] to messageResourceUtils
    bind[JsonDeserializationErrorMessageCreator] to jsonDeserializationErrorMessageCreator
    bind[StreamSI] to serviceInterface
    bind[StreamApiCreator] to createStreamApi
  }.injector

  val controller = new StreamController()(injector)


  // create
  "StreamController" should "tell that valid stream created" in {
    val expected = CreatedRestResponse(MessageResponseEntity(createdMessage))
    controller.create(newStream.serialized) shouldBe expected
  }

  it should "tell that stream is not a valid" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(notValidMessage))
    controller.create(notValidStream.serialized) shouldBe expected
  }

  it should "tell that serialized to json stream is not a correct" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(incorrectJsonMessage))
    controller.create(incorrectJson) shouldBe expected
  }

  // getAll
  it should "give all streams" in {
    val expected = OkRestResponse(StreamsResponseEntity(allStreams.map(_.api).toBuffer))
    controller.getAll() shouldBe expected
  }

  // get
  it should "give stream if it is exists" in {
    allStreams.foreach {
      case StreamInfo(_, api, _, name) =>
        controller.get(name) shouldBe OkRestResponse(StreamResponseEntity(api))
    }
  }

  it should "not give stream if it does not exists" in {
    controller.get(newStreamName) shouldBe entityNotFoundResponse
  }

  // getRelated
  it should "give instances related to stream" in {
    val expected = OkRestResponse(RelatedToStreamResponseEntity(relatedInstances))
    controller.getRelated(withRelatedStreamName) shouldBe expected
  }

  it should "give empty buffer if stream does not have related streams" in {
    allStreams.foreach {
      case StreamInfo(_, _, _, name) =>
        controller.getRelated(name) shouldBe OkRestResponse(RelatedToStreamResponseEntity())
    }
  }

  it should "tell that stream does not exists (getRelated)" in {
    controller.getRelated(newStreamName) shouldBe entityNotFoundResponse
  }

  // delete
  it should "delete stream if it exists" in {
    allStreams.foreach {
      case StreamInfo(_, _, _, name) =>
        val message = entityDeletedMessageName + "," + name
        when(messageResourceUtils.createMessage(entityDeletedMessageName, name)).thenReturn(message)

        controller.delete(name) shouldBe OkRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not delete stream if it does not exists" in {
    controller.delete(newStreamName) shouldBe entityNotFoundResponse
  }

  it should "report in a case some deletion error" in {
    val expected = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))
    controller.delete(notDeletedStreamName) shouldBe expected
  }

  // getTypes
  it should "get all stream types" in {
    val expected = OkRestResponse(TypesResponseEntity(beToFeTypes.map(x => Type(x._1, x._2)).toSeq))
    controller.getTypes shouldBe expected
  }


  def createStream(name: String) = {
    val streamType = tstreamsType
    val serialized = s"""{"name":"$name","type":"$streamType"}"""

    val stream = mock[SjStream]
    when(stream.name).thenReturn(name)
    when(stream.streamType).thenReturn(streamType)

    val api = mock[StreamApi]
    when(api.name).thenReturn(name)
    when(api.streamType).thenReturn(streamType)
    when(api.to(any[Injector]())).thenReturn(stream)

    when(serializer.deserialize[StreamApi](serialized)).thenReturn(api)
    when(createStreamApi.from(stream)).thenReturn(api)

    StreamInfo(serialized, api, stream, name)
  }

  case class StreamInfo(serialized: String, api: StreamApi, stream: SjStream, name: String)

}
