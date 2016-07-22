package com.bwsw.sj.crud.rest

import java.io.FileNotFoundException
import java.text.MessageFormat

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{EntityStreamSizeException, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import com.bwsw.common.exceptions._
import com.bwsw.sj.crud.rest.api._
import com.bwsw.sj.crud.rest.cors.CorsSupport
import com.bwsw.sj.crud.rest.entities.ProtocolResponse
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
    case BadRecord(msg) =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(400, Map("message" -> msg))))
      ))
    case BadRecordWithKey(msg, key) =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(400, Map("message" -> msg))))
      ))
    case NotFoundException(msg, key) =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(400, Map("message" -> msg))))
      ))
    case InstanceException(msg, key) =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(400, Map("message" -> msg))))
      ))
    case KeyAlreadyExists(msg, key) =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(400, Map("message" -> msg))))
      ))
    case ex: ValidationException =>
      complete(HttpResponse(
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(500, Map("message" -> messages.getString("rest.errors.invalid.specification")))))
      ))
    case ex: EntityStreamSizeException =>
      complete(HttpResponse(
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(500, Map("message" -> messages.getString("rest.errors.large_file")))))
      ))
    case ex: FileNotFoundException =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(400, Map("message" -> ex.getMessage))))
      ))
    case ex: UnrecognizedPropertyException =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(500, Map("message" -> MessageFormat.format(
          messages.getString("rest.errors.unrecognized_property"), ex.getPropertyName, ex.getKnownPropertyIds)))))
      ))
    case ex: Exception =>
      ex.printStackTrace()
      complete(HttpResponse(
        InternalServerError,
        entity = HttpEntity(`application/json`, serializer.serialize(ProtocolResponse(500, Map("message" -> MessageFormat.format(
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
