package com.bwsw.sj.crud.rest

import akka.http.scaladsl.model.EntityStreamSizeException
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import com.bwsw.sj.common.rest.entities.{InternalServerErrorRestResponse, NotFoundRestResponse}
import com.bwsw.sj.crud.rest.api._
import com.bwsw.sj.crud.rest.cors.CorsSupport
import com.bwsw.sj.crud.rest.exceptions._
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import org.everit.json.schema.ValidationException
import scala.concurrent.duration._

/**
 * Route for CRUD Rest-API
 *
 *
 * @author Kseniya Tomskikh
 */
trait SjCrudInterface extends Directives
with CorsSupport
with SjModulesApi
with SjCustomApi
with SjStreamsApi
with SjServicesApi
with SjProvidersApi
with SjConfigurationSettingsApi {
  val exceptionHandler = ExceptionHandler {
    case InstanceNotFound(msg, key) =>
      val response = NotFoundRestResponse(Map("message" -> msg, "key" -> key))
      complete(restResponseToHttpResponse(response))
    case ModuleNotFound(msg, key) =>
      val response = NotFoundRestResponse(Map("message" -> msg, "key" -> key))
      complete(restResponseToHttpResponse(response))
    case ModuleJarNotFound(msg, key) =>
      val response = NotFoundRestResponse(Map("message" -> msg, "key" -> key))
      complete(restResponseToHttpResponse(response))
    case UnknownModuleType(msg, key) =>
      val response = NotFoundRestResponse(Map("message" -> msg, "key" -> key))
      complete(restResponseToHttpResponse(response))
    case ConfigSettingNotFound(msg) =>
      val response = NotFoundRestResponse(Map("message" -> msg))
      complete(restResponseToHttpResponse(response))
    case ex: EntityStreamSizeException =>
      val response = InternalServerErrorRestResponse(Map("message" -> getMessage("rest.errors.large_file")))
      complete(restResponseToHttpResponse(response))
    case ex: UnrecognizedPropertyException =>
      val response = InternalServerErrorRestResponse(Map("message" ->
        createMessage("rest.errors.unrecognized_property", ex.getPropertyName, ex.getKnownPropertyIds.toArray.mkString(", "))))
      complete(restResponseToHttpResponse(response))
    case jsonSchemaValidationException:ValidationException =>
      val response = InternalServerErrorRestResponse(Map("message" ->
        createMessage("rest.errors.entity.json.schema.failed", s"${jsonSchemaValidationException.getMessage}")))
      complete(restResponseToHttpResponse(response))
    case ex: Exception =>
      ex.printStackTrace()
      val response = InternalServerErrorRestResponse(Map("message" ->
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
