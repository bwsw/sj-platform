package com.bwsw.sj.crud.rest

import java.text.MessageFormat

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{EntityStreamSizeException, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import com.bwsw.common.exceptions._
import com.bwsw.sj.crud.rest.api._
import com.bwsw.sj.crud.rest.cors.CorsSupport
import com.bwsw.sj.crud.rest.entities.{BadRequestRestResponse, InternalServerErrorRestResponse}
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import org.everit.json.schema.ValidationException

/**
 * Router trait for CRUD Rest-API
 * Created: 06/04/2016
 *
 * @author Kseniya Tomskikh
 */
trait SjCrudRouter extends Directives
with CorsSupport
with SjModulesApi
with SjCustomApi
with SjStreamsApi
with SjServicesApi
with SjProvidersApi
with SjConfigSettingsApi {

  val exceptionHandler = ExceptionHandler {
    case BadRequest(msg) =>
      complete(HttpResponse(
        StatusCodes.BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(BadRequestRestResponse(Map("message" -> msg))))
      ))
    case BadRequestWithKey(msg, key) =>
      complete(HttpResponse(
        StatusCodes.BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(BadRequestRestResponse(Map("message" -> msg))))
      ))
    case InstanceException(msg, key) =>
      complete(HttpResponse(
        StatusCodes.BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(BadRequestRestResponse(Map("message" -> msg))))
      ))
    case ex: ValidationException =>
      complete(HttpResponse(
        StatusCodes.InternalServerError,
        entity = HttpEntity(`application/json`, serializer.serialize(InternalServerErrorRestResponse(Map("message" -> messages.getString("rest.errors.invalid.specification")))))
      ))
    case ex: EntityStreamSizeException =>
      complete(HttpResponse(
        StatusCodes.InternalServerError,
        entity = HttpEntity(`application/json`, serializer.serialize(InternalServerErrorRestResponse(Map("message" -> messages.getString("rest.errors.large_file")))))
      ))
    case ex: UnrecognizedPropertyException =>
      complete(HttpResponse(
        StatusCodes.InternalServerError,
        entity = HttpEntity(`application/json`, serializer.serialize(InternalServerErrorRestResponse(Map("message" -> MessageFormat.format(
          messages.getString("rest.errors.unrecognized_property"), ex.getPropertyName, ex.getKnownPropertyIds)))))
      ))
    case ex: Exception =>
      ex.printStackTrace()
      complete(HttpResponse(
        StatusCodes.InternalServerError,
        entity = HttpEntity(`application/json`, serializer.serialize(InternalServerErrorRestResponse(Map("message" -> MessageFormat.format(
          messages.getString("rest.errors.internal_server_error"), ex.getMessage)))))
      ))
  }

  def route() = {
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
