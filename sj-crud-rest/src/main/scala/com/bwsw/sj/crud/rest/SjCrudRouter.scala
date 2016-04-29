package com.bwsw.sj.crud.rest

import java.io.FileNotFoundException

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{EntityStreamSizeException, HttpEntity, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{ExceptionHandler, Directives}
import com.bwsw.common.exceptions.{InstanceException, BadRecordWithKey, BadRecord}
import com.bwsw.sj.crud.rest.entities.Response
import com.bwsw.sj.crud.rest.api.{SjCustomApi, SjModulesApi}
import org.everit.json.schema.ValidationException

/**
  * Router trait for CRUD Rest-API
  * Created: 06/04/2016
  *
  * @author Kseniya Tomskikh
  */
trait SjCrudRouter extends Directives
  with SjModulesApi
  with SjCustomApi {

  val exceptionHandler = ExceptionHandler {
    case BadRecord(msg) =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(Response(400, null, msg)))
      ))
    case BadRecordWithKey(msg, key) =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(Response(400, key, msg)))
      ))
    case InstanceException(msg, key) =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(Response(400, key, msg)))
      ))
    case ex: ValidationException =>
      complete(HttpResponse(
        entity = HttpEntity(`application/json`, serializer.serialize(Response(500, null, "Specification.json is invalid")))
      ))
    case ex: EntityStreamSizeException =>
      complete(HttpResponse(
        entity = HttpEntity(`application/json`, serializer.serialize(Response(500, null, "File is very large")))
      ))
    case ex: FileNotFoundException =>
      complete(HttpResponse(
        BadRequest,
        entity = HttpEntity(`application/json`, serializer.serialize(Response(400, null, ex.getMessage)))
      ))
    case ex: Exception =>
      complete(HttpResponse(
        InternalServerError,
        entity = HttpEntity(`application/json`, serializer.serialize(Response(500, null, "Internal server error: " + ex.getMessage)))
      ))
  }

  def route() = {
    handleExceptions(exceptionHandler) {
      pathPrefix("v1") {
        modulesApi ~
        customApi
      }
    }
  }

}
