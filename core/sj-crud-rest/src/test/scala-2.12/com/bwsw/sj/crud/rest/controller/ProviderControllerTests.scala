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
import com.bwsw.sj.common.si.ProviderSI
import com.bwsw.sj.common.si.model.provider.Provider
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.common.utils.ProviderLiterals._
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.provider.{ProviderApiCreator, ProviderApi}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ProviderControllerTests extends FlatSpec with Matchers with MockitoSugar {
  val entityDeletedMessageName = "rest.providers.provider.deleted"
  val entityNotFoundMessageName = "rest.providers.provider.notfound"
  val createdMessageName = "rest.providers.provider.created"
  val cannotCreateMessageName = "rest.providers.provider.cannot.create"

  val creationError = ArrayBuffer("not created")

  val serializer = mock[JsonSerializer]

  val messageResourceUtils = mock[MessageResourceUtils]
  val createProviderApi = mock[ProviderApiCreator]

  val serviceInterface = mock[ProviderSI]
  when(serviceInterface.get(anyString())).thenReturn(None)
  when(serviceInterface.delete(anyString())).thenReturn(EntityNotFound)
  when(serviceInterface.create(any[Provider]())).thenReturn(NotCreated(creationError))
  when(serviceInterface.checkConnection(anyString())).thenReturn(Right(false))
  when(serviceInterface.getRelated(anyString())).thenReturn(Left(false))

  val jsonDeserializationErrorMessageCreator = mock[JsonDeserializationErrorMessageCreator]

  val providersCount = 3
  val allProviders = Range(0, providersCount).map(i => createProvider(s"name$i"))
  allProviders.foreach {
    case ProviderInfo(_, _, provider, name) =>
      when(serviceInterface.get(name)).thenReturn(Some(provider))
      when(serviceInterface.delete(name)).thenReturn(Deleted)
      when(serviceInterface.getRelated(name)).thenReturn(Right(mutable.Buffer.empty[String]))
      when(serviceInterface.checkConnection(name)).thenReturn(Right(true))
  }
  when(serviceInterface.getAll()).thenReturn(allProviders.map(_.provider).toBuffer)

  val connectionFailedProviderName = "connection-failed-provider"
  val connectionFailedError = "connection failed"
  when(serviceInterface.checkConnection(connectionFailedProviderName))
    .thenReturn(Left(ArrayBuffer(connectionFailedError)))

  val withRelatedProviderName = "with-related-provider"
  val relatedServices = mutable.Buffer("service1, service2, service3")
  when(serviceInterface.getRelated(withRelatedProviderName)).thenReturn(Right(relatedServices))

  val entityDeletedMessages = allProviders.map {
    case ProviderInfo(_, _, _, name) =>
      val message = entityDeletedMessageName + "," + name
      when(messageResourceUtils.createMessage(entityDeletedMessageName, name)).thenReturn(message)

      (name, message)
  }

  val newProviderName = "new-provider-name"
  val newProvider = createProvider(newProviderName)
  when(serviceInterface.create(newProvider.provider)).thenReturn(Created)

  val createdMessage = createdMessageName + "," + newProviderName
  when(messageResourceUtils.createMessage(createdMessageName, newProviderName)).thenReturn(createdMessage)

  val entityNotFoundMessage = entityNotFoundMessageName + "," + newProviderName
  val entityNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
  when(messageResourceUtils.createMessage(entityNotFoundMessageName, newProviderName))
    .thenReturn(entityNotFoundMessage)

  val notValidProviderName = "not-valid-provider-name"
  val notValidProvider = createProvider(notValidProviderName)

  val notValidMessage = cannotCreateMessageName + "," + notValidProviderName
  when(messageResourceUtils.createMessageWithErrors(cannotCreateMessageName, creationError)).thenReturn(notValidMessage)

  val notDeletedProviderName = "not-deleted-provider-name"
  val notDeletedProvider = createProvider(notDeletedProviderName)
  val deletionError = "provider-not-deleted"
  when(serviceInterface.delete(notDeletedProviderName)).thenReturn(DeletionError(deletionError))

  val incorrectJson = "{not a json}"
  val incorrectJsonException = new JsonDeserializationException("json is incorrect")
  val incorrectJsonError = "error: json is incorrect"
  when(serializer.deserialize[ProviderApi](incorrectJson)).thenAnswer(_ => throw incorrectJsonException)
  when(jsonDeserializationErrorMessageCreator.apply(incorrectJsonException)).thenReturn(incorrectJsonError)

  val incorrectJsonMessage = cannotCreateMessageName + "," + incorrectJson
  when(messageResourceUtils.createMessage(cannotCreateMessageName, incorrectJsonError)).thenReturn(incorrectJsonMessage)

  val injector = new Module {
    bind[JsonSerializer] to serializer
    bind[MessageResourceUtils] to messageResourceUtils
    bind[JsonDeserializationErrorMessageCreator] to jsonDeserializationErrorMessageCreator
    bind[ProviderSI] to serviceInterface
    bind[ProviderApiCreator] to createProviderApi
  }.injector

  val controller = new ProviderController()(injector)


  // create
  "ProviderController" should "tell that valid provider created" in {
    val expected = CreatedRestResponse(MessageResponseEntity(createdMessage))
    controller.create(newProvider.serialized) shouldBe expected
  }

  it should "tell that provider is not a valid" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(notValidMessage))
    controller.create(notValidProvider.serialized) shouldBe expected
  }

  it should "tell that serialized to json provider is not a correct" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(incorrectJsonMessage))
    controller.create(incorrectJson) shouldBe expected
  }

  // getAll
  it should "give all providers" in {
    val expected = OkRestResponse(ProvidersResponseEntity(allProviders.map(_.api).toBuffer))
    controller.getAll() shouldBe expected
  }

  // get
  it should "give provider if it is exists" in {
    allProviders.foreach {
      case ProviderInfo(_, api, _, name) =>
        controller.get(name) shouldBe OkRestResponse(ProviderResponseEntity(api))
    }
  }

  it should "not give provider if it does not exists" in {
    controller.get(newProviderName) shouldBe entityNotFoundResponse
  }

  // checkConnection
  it should "tell that provider can connect to server" in {
    allProviders.foreach {
      case ProviderInfo(_, _, _, name) =>
        controller.checkConnection(name) shouldBe OkRestResponse(ConnectionResponseEntity())
    }
  }

  it should "tell that provider cannot connect to server" in {
    val expected = ConflictRestResponse(TestConnectionResponseEntity(connection = false, connectionFailedError))
    controller.checkConnection(connectionFailedProviderName) shouldBe expected
  }

  it should "tell that provider does not exists (checkConnection)" in {
    controller.checkConnection(newProviderName) shouldBe entityNotFoundResponse
  }

  // getRelated
  it should "give services related to provider" in {
    val expected = OkRestResponse(RelatedToProviderResponseEntity(relatedServices))
    controller.getRelated(withRelatedProviderName) shouldBe expected
  }

  it should "give empty buffer if provider does not have related services" in {
    allProviders.foreach {
      case ProviderInfo(_, _, _, name) =>
        controller.getRelated(name) shouldBe OkRestResponse(RelatedToProviderResponseEntity())
    }
  }

  it should "tell that provider does not exists (getRelated)" in {
    controller.getRelated(newProviderName) shouldBe entityNotFoundResponse
  }

  // delete
  it should "delete provider if it exists" in {
    allProviders.foreach {
      case ProviderInfo(_, _, _, name) =>
        val message = entityDeletedMessageName + "," + name
        when(messageResourceUtils.createMessage(entityDeletedMessageName, name)).thenReturn(message)

        controller.delete(name) shouldBe OkRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not delete provider if it does not exists" in {
    controller.delete(newProviderName) shouldBe entityNotFoundResponse
  }

  it should "report in a case some deletion error" in {
    val expected = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))
    controller.delete(notDeletedProviderName) shouldBe expected
  }

  // getTypes
  it should "get all provider types" in {
    val expected = OkRestResponse(TypesResponseEntity(types))
    controller.getTypes() shouldBe expected
  }


  def createProvider(name: String) = {
    val providerType = zookeeperType
    val serialized = s"""{"name":"$name","type":"$providerType"}"""

    val provider = mock[Provider]
    when(provider.name).thenReturn(name)
    when(provider.providerType).thenReturn(providerType)

    val api = mock[ProviderApi]
    when(api.name).thenReturn(name)
    when(api.providerType).thenReturn(providerType)
    when(api.to()(any[Injector]())).thenReturn(provider)

    when(serializer.deserialize[ProviderApi](serialized)).thenReturn(api)
    when(createProviderApi.from(provider)).thenReturn(api)

    ProviderInfo(serialized, api, provider, name)
  }

  case class ProviderInfo(serialized: String, api: ProviderApi, provider: Provider, name: String)

}
