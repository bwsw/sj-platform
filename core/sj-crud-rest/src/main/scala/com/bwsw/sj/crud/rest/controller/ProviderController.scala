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

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ProviderSI
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.provider.{ProviderApiCreator, ProviderApi}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class ProviderController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  override val serviceInterface = inject[ProviderSI]

  override protected val entityDeletedMessage: String = "rest.providers.provider.deleted"
  override protected val entityNotFoundMessage: String = "rest.providers.provider.notfound"

  private val createProviderApi = inject[ProviderApiCreator]

  def create(serializedEntity: String): RestResponse = {
    val triedProviderApi = Try(serializer.deserialize[ProviderApi](serializedEntity))
    triedProviderApi match {
      case Success(providerData) =>
        val created = serviceInterface.create(providerData.to())

        created match {
          case Created =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.created", providerData.name)))
          case NotCreated(errors) =>
            BadRequestRestResponse(MessageResponseEntity(
              createMessageWithErrors("rest.providers.provider.cannot.create", errors)))
        }
      case Failure(exception: JsonDeserializationException) =>
        val error = jsonDeserializationErrorMessageCreator(exception)
        BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.providers.provider.cannot.create", error)))

      case Failure(exception) => throw exception
    }
  }

  def getAll(): RestResponse = {
    val providers = serviceInterface.getAll().map(createProviderApi.from)
    OkRestResponse(ProvidersResponseEntity(providers))
  }

  def get(name: String): RestResponse = {
    val provider = serviceInterface.get(name)

    provider match {
      case Some(x) =>
        OkRestResponse(ProviderResponseEntity(createProviderApi.from(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  def checkConnection(name: String): RestResponse = {
    val connectionResponse = serviceInterface.checkConnection(name)

    connectionResponse match {
      case Right(isFound) =>
        if (isFound) {
          OkRestResponse(ConnectionResponseEntity())
        }
        else {
          NotFoundRestResponse(
            MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
        }
      case Left(message) =>
        ConflictRestResponse(TestConnectionResponseEntity(connection = false, message.mkString(";")))
    }
  }

  def getRelated(name: String): RestResponse = {
    val relatedServices = serviceInterface.getRelated(name)
    relatedServices match {
      case Right(services) =>
        OkRestResponse(RelatedToProviderResponseEntity(services))
      case Left(_) =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  def getTypes(): RestResponse = {
    OkRestResponse(TypesResponseEntity(ProviderLiterals.beToFeTypes.map(x => Type(x._1, x._2)).toSeq))
  }
}


