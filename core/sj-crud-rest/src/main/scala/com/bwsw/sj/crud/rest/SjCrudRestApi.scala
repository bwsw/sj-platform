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
package com.bwsw.sj.crud.rest

import akka.http.scaladsl.model.EntityStreamSizeException
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import com.bwsw.sj.common.rest.{InternalServerErrorRestResponse, KeyedMessageResponseEntity, MessageResponseEntity, NotFoundRestResponse}
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.cors.CorsSupport
import com.bwsw.sj.crud.rest.exceptions._
import com.bwsw.sj.crud.rest.routes._
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import org.everit.json.schema.ValidationException
import scaldi.Injectable.inject

import scala.concurrent.duration._

/**
  * Route for CRUD Rest-API
  *
  * @author Kseniya Tomskikh
  */
trait SjCrudRestApi extends Directives
  with CorsSupport
  with SjModulesRoute
  with SjCustomRoute
  with SjStreamsRoute
  with SjServicesRoute
  with SjProvidersRoute
  with SjConfigurationSettingsRoute {

  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  val exceptionHandler = ExceptionHandler {
    case InstanceNotFound(msg, key) =>
      val response = NotFoundRestResponse(KeyedMessageResponseEntity(msg, key))
      complete(restResponseToHttpResponse(response))
    case ModuleNotFound(msg, key) =>
      val response = NotFoundRestResponse(KeyedMessageResponseEntity(msg, key))
      complete(restResponseToHttpResponse(response))
    case ModuleJarNotFound(msg, key) =>
      val response = NotFoundRestResponse(KeyedMessageResponseEntity(msg, key))
      complete(restResponseToHttpResponse(response))
    case UnknownModuleType(msg, key) =>
      val response = NotFoundRestResponse(KeyedMessageResponseEntity(msg, key))
      complete(restResponseToHttpResponse(response))
    case ConfigSettingNotFound(msg) =>
      val response = NotFoundRestResponse(MessageResponseEntity(msg))
      complete(restResponseToHttpResponse(response))
    case ex: EntityStreamSizeException =>
      val response = InternalServerErrorRestResponse(MessageResponseEntity(getMessage("rest.errors.large_file")))
      complete(restResponseToHttpResponse(response))
    case ex: UnrecognizedPropertyException =>
      val response = InternalServerErrorRestResponse(MessageResponseEntity(
        createMessage("rest.errors.unrecognized_property", ex.getPropertyName, ex.getKnownPropertyIds.toArray.mkString(", "))))
      complete(restResponseToHttpResponse(response))
    case jsonSchemaValidationException: ValidationException =>
      val response = InternalServerErrorRestResponse(MessageResponseEntity(
        createMessage("rest.errors.entity.json.schema.failed", s"${jsonSchemaValidationException.getMessage}")))
      complete(restResponseToHttpResponse(response))
    case ex: Exception =>
      ex.printStackTrace()
      val response = InternalServerErrorRestResponse(MessageResponseEntity(
        createMessage("rest.errors.internal_server_error", ex.getMessage)))
      complete(restResponseToHttpResponse(response))
  }

  def route(): Route = {
    handleExceptions(exceptionHandler) {
      corsHandler {
        pathPrefix("v1") {
          withRequestTimeout(30.seconds) {
            modulesRoute ~
              customRoute ~
              streamsRoute ~
              servicesRoute ~
              providersRoute ~
              configSettingsRoute
          }
        }
      }
    }
  }
}
