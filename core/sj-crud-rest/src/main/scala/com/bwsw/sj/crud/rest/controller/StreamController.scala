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
import com.bwsw.sj.common.si.StreamSI
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.{MessageResourceUtils, StreamLiterals}
import com.bwsw.sj.crud.rest.model.stream.{StreamApi, StreamApiCreator}
import com.bwsw.sj.crud.rest.{RelatedToStreamResponseEntity, StreamResponseEntity, StreamsResponseEntity}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class StreamController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  override val serviceInterface = inject[StreamSI]

  protected val entityNotFoundMessage: String = "rest.streams.stream.notfound"
  protected val entityDeletedMessage: String = "rest.streams.stream.deleted"
  private val createStreamApi = inject[StreamApiCreator]

  override def create(serializedEntity: String): RestResponse = {
    Try(serializer.deserialize[StreamApi](serializedEntity)) match {
      case Success(streamData) =>
        serviceInterface.create(streamData.to) match {
          case Created =>
            CreatedRestResponse(
              MessageResponseEntity(
                createMessage("rest.streams.stream.created", streamData.name)))
          case NotCreated(errors) =>
            BadRequestRestResponse(
              MessageResponseEntity(
                createMessageWithErrors("rest.streams.stream.cannot.create", errors)))
        }

      case Failure(exception: JsonDeserializationException) =>
        val error = jsonDeserializationErrorMessageCreator(exception)
        BadRequestRestResponse(
          MessageResponseEntity(
            createMessage("rest.streams.stream.cannot.create", error)))

      case Failure(exception) => throw exception
    }
  }

  override def get(name: String): RestResponse = {
    serviceInterface.get(name) match {
      case Some(stream) =>
        OkRestResponse(StreamResponseEntity(createStreamApi.from(stream)))
      case None =>
        NotFoundRestResponse(
          MessageResponseEntity(
            createMessage(entityNotFoundMessage, name)))
    }
  }

  override def getAll(): RestResponse = {
    val streams = serviceInterface.getAll()
    val responseEntity = StreamsResponseEntity(streams.map(createStreamApi.from))
    OkRestResponse(responseEntity)
  }

  def getTypes = {
    OkRestResponse(TypesResponseEntity(StreamLiterals.beToFeTypes.map(x => Type(x._1, x._2)).toSeq))
  }

  def getRelated(name: String): RestResponse = {
    serviceInterface.getRelated(name) match {
      case Some(related) =>
        OkRestResponse(RelatedToStreamResponseEntity(related))
      case None =>
        NotFoundRestResponse(
          MessageResponseEntity(
            createMessage(entityNotFoundMessage, name)))
    }
  }
}
