package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model.ConfigElement
import com.bwsw.sj.crud.rest.entities.{ConfigElementData, ProtocolResponse}
import com.bwsw.sj.crud.rest.utils.ConvertUtil._
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.config.file.ConfigFileValidator

/**
 * Rest-api for config file
 *
 */
trait SjConfigFileApi extends Directives with SjCrudValidator {

  val providersApi = {
    pathPrefix("config") {
      pathPrefix("file") {
        pathEndOrSingleSlash {
          post { (ctx: RequestContext) =>
            val data = serializer.deserialize[ConfigElementData](getEntityFromContext(ctx))

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
              var response: ProtocolResponse = null
              if (configElements.nonEmpty) {
                val entity = Map("config-elements" -> configElements.map(element => configElementToConfigElementData(element)))
                response = ProtocolResponse(200, entity)
              } else {
                response = ProtocolResponse(200, Map("message" -> "Config file is empty"))
              }
              complete(HttpEntity(`application/json`, serializer.serialize(response)))
            }
        } ~
          pathPrefix(Segment) { (name: String) =>
            pathEndOrSingleSlash {
              get {
                val configElement = configFileService.get(name)
                var response: ProtocolResponse = null
                if (configElement != null) {
                  val entity = Map("config-elements" -> configElementToConfigElementData(configElement))
                  response = ProtocolResponse(200, entity)
                } else {
                  response = ProtocolResponse(200, Map("message" -> s"Config element '$name' not found"))
                }
                complete(HttpEntity(`application/json`, serializer.serialize(response)))
              } ~
//                put { (ctx: RequestContext) =>
//                    val data = serializer.deserialize[ConfigElementData](getEntityFromContext(ctx))
//
//                    val errors = ConfigFileValidator.validate(data)
//
//                    if (errors.isEmpty) {
//                      val configElement = new ConfigElement(
//                        data.name,
//                        data.value
//                      )
//                      configFileService.save(configElement)
//                      val response = ProtocolResponse(200, Map("message" -> s"Config element '${configElement.name}' is created"))
//                      ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
//                    } else {
//                      throw new BadRecordWithKey(
//                        s"Cannot create config element. Errors: ${errors.mkString("\n")}",
//                        s"${data.name}"
//                      )
//                    }
//                  var response: ProtocolResponse = null
//                  if (configFileService.get(name) != null) {
//                    val entity = Map("message" -> s"Config element '$name' has been deleted")
//                    response = ProtocolResponse(200, entity)
//                    configFileService.delete(name)
//                  } else {
//                    response = ProtocolResponse(200, Map("message" -> s"Config element '$name' not found"))
//                  }
//                  complete(HttpEntity(`application/json`, serializer.serialize(response)))
//                } ~
                delete {
                  var response: ProtocolResponse = null
                  if (configFileService.get(name) != null) {
                    val entity = Map("message" -> s"Config element '$name' has been deleted")
                    response = ProtocolResponse(200, entity)
                    configFileService.delete(name)
                  } else {
                    response = ProtocolResponse(200, Map("message" -> s"Config element '$name' not found"))
                  }
                  complete(HttpEntity(`application/json`, serializer.serialize(response)))
                }
            }
          }
      }
    }
  }
}

