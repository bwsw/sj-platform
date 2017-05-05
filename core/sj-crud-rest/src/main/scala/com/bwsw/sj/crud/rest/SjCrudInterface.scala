package com.bwsw.sj.crud.rest

import akka.http.scaladsl.model.EntityStreamSizeException
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import com.bwsw.sj.common.rest.{InternalServerErrorRestResponse, KeyedMessageResponseEntity, MessageResponseEntity, NotFoundRestResponse}
import com.bwsw.sj.crud.rest.routes._
import com.bwsw.sj.crud.rest.cors.CorsSupport
import com.bwsw.sj.crud.rest.exceptions._
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import org.everit.json.schema.ValidationException

import scala.concurrent.duration._
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._

/**
 * Route for CRUD Rest-API
 *
 *
 * @author Kseniya Tomskikh
 */
trait SjCrudInterface extends Directives
with CorsSupport
with SjModulesRoute
with SjCustomRoute
with SjStreamsRoute
with SjServicesRoute
with SjProvidersRoute
with SjConfigurationSettingsRoute {
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
    case jsonSchemaValidationException:ValidationException =>
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
            modulesApi ~
              customApi ~
              streamsApi ~
              servicesApi ~
              providersApi ~
              configSettingsApi
          }
        }
      }
    }
  }
}
