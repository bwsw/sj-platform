package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model.ConfigElement
import com.bwsw.sj.crud.rest.entities.ProtocolResponse
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.config.file.ConfigFileValidator

/**
 * Rest-api for config file
 *
 */
trait SjConfigFileApi extends Directives with SjCrudValidator {

  val configFileApi = {
    pathPrefix("config") {
      pathPrefix("file") {
        pathEndOrSingleSlash {
          post { (ctx: RequestContext) =>
            val data = serializer.deserialize[ConfigElement](getEntityFromContext(ctx))

            val errors = ConfigFileValidator.validate(data)

            if (errors.isEmpty) {
              val configElement = new ConfigElement(
                data.name,
                data.value
              )
              configFileService.save(configElement)
              val response = ProtocolResponse(200, Map("message" -> s"Config element '${configElement.name}' is created"))
              ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
            } else {
              throw new BadRecordWithKey(
                s"Cannot create config element. Errors: ${errors.mkString("\n")}",
                s"${data.name}"
              )
            }
          } ~
            get {
              val configElements = configFileService.getAll
              var response: Option[ProtocolResponse] = None
              if (configElements.nonEmpty) {
                val entity = Map("config-elements" -> configElements)
                response = Some(ProtocolResponse(200, entity))
              } else {
                response = Some(ProtocolResponse(200, Map("message" -> "Config file is empty")))
              }
              response match {
                case None => throw new Exception("Something was going seriously wrong")
                case Some(x) => complete(HttpEntity(`application/json`, serializer.serialize(x)))
              }
            }
        } ~
          pathPrefix(Segment) { (name: String) =>
            pathEndOrSingleSlash {
              get {
                val configElement = configFileService.get(name)
                var response: Option[ProtocolResponse] = None
                if (configElement != null) {
                  val entity = Map("config-elements" -> configElement)
                  response = Some(ProtocolResponse(200, entity))
                } else {
                  response = Some(ProtocolResponse(200, Map("message" -> s"Config element '$name' has not found")))
                }
                response match {
                  case None => throw new Exception("Something was going seriously wrong")
                  case Some(x) => complete(HttpEntity(`application/json`, serializer.serialize(x)))
                }
              } ~
                delete {
                  var response: Option[ProtocolResponse] = None
                  if (configFileService.get(name) != null) {
                    val entity = Map("message" -> s"Config element '$name' has been deleted")
                    response = Some(ProtocolResponse(200, entity))
                    configFileService.delete(name)
                  } else {
                    response = Some(ProtocolResponse(200, Map("message" -> s"Config element '$name' has not found")))
                  }
                  response match {
                    case None => throw new Exception("Something was going seriously wrong")
                    case Some(x) => complete(HttpEntity(`application/json`, serializer.serialize(x)))
                  }
                }
            }
          }
      }
    }
  }
}

