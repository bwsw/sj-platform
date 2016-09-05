package com.bwsw.sj.crud.rest

import java.text.MessageFormat

import akka.http.scaladsl.model.EntityStreamSizeException
import akka.http.scaladsl.server.{Route, Directives, ExceptionHandler}
import com.bwsw.sj.crud.rest.api._
import com.bwsw.sj.crud.rest.cors.CorsSupport
import com.bwsw.sj.common.rest.entities.{InternalServerErrorRestResponse, NotFoundRestResponse}
import com.bwsw.sj.crud.rest.exceptions._
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import org.everit.json.schema.ValidationException

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
with SjConfigurationSettingsApi with CompletionUtils {

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
    case ex: ValidationException =>
      val response = InternalServerErrorRestResponse(Map("message" -> messages.getString("rest.errors.invalid.specification")))
      complete(restResponseToHttpResponse(response))
    case ex: EntityStreamSizeException =>
      val response = InternalServerErrorRestResponse(Map("message" -> messages.getString("rest.errors.large_file")))
      complete(restResponseToHttpResponse(response))
    case ex: UnrecognizedPropertyException =>
      val response = InternalServerErrorRestResponse(Map("message" -> MessageFormat.format(
        messages.getString("rest.errors.unrecognized_property"), ex.getPropertyName, ex.getKnownPropertyIds)))
      complete(restResponseToHttpResponse(response))
    case ex: Exception =>
      ex.printStackTrace()
      val response = InternalServerErrorRestResponse(Map("message" -> MessageFormat.format(
        messages.getString("rest.errors.internal_server_error"), ex.getMessage)))
      complete(restResponseToHttpResponse(response))
  }

  def route(): Route = {
    handleExceptions(exceptionHandler) {
      corsHandler {
        pathPrefix("v1") {
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
