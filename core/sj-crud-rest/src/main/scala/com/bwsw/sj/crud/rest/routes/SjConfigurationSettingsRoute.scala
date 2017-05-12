package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.rest.model.config.ConfigurationSettingApi
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.crud.rest.controller.ConfigSettingsController
import com.bwsw.sj.crud.rest.exceptions.UnknownConfigSettingDomain
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.config.ConfigurationSettingApi

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

trait SjConfigurationSettingsRoute extends Directives with SjCrudValidator {
  val configSettingsController = new ConfigSettingsController

  val configSettingsRoute = {
    pathPrefix("config") {
      pathPrefix("settings") {
        pathPrefix("domains") {
          pathEndOrSingleSlash {
            get {
              val response = OkRestResponse(DomainsResponseEntity())
              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
        pathPrefix(Segment) { (domain: String) =>
          if (!ConfigLiterals.domains.contains(domain))
            throw UnknownConfigSettingDomain(createMessage("rest.config.setting.domain.unknown",
              ConfigLiterals.domains.mkString(", ")), domain)

          pathEndOrSingleSlash {
            get {
//              val configElements = configService.getByParameters(Map("domain" -> domain))
//              val response = OkRestResponse(ConfigSettingsResponseEntity())
//              if (configElements.nonEmpty) {
//                response.entity = ConfigSettingsResponseEntity(configElements.map(cs => ConfigurationSettingApi.from(cs)))
//              }
              val response = configSettingsController.getDomain(domain)
              complete(restResponseToHttpResponse(response))
            }
          } ~
          pathPrefix(Segment) { (name: String) =>
            pathEndOrSingleSlash {
              get {
                val response = configSettingsController.get(createConfigurationSettingName(domain, name))
                complete(restResponseToHttpResponse(response))
              } ~
              delete {
                val response = configSettingsController.delete(createConfigurationSettingName(domain, name))
                complete(restResponseToHttpResponse(response))
              }
            }
          }
        } ~
        pathEndOrSingleSlash {
          post { (ctx: RequestContext) =>
            val entity = getEntityFromContext(ctx)
            val response = configSettingsController.create(entity)
            ctx.complete(restResponseToHttpResponse(response))
          } ~
          get {
            val response = configSettingsController.getAll()
            complete(restResponseToHttpResponse(response))
          }
        }
      }
    }




//    pathPrefix("config") {
//      pathPrefix("settings") {
//        pathPrefix("domains") {
//          pathEndOrSingleSlash {
//            get {
//              val response = OkRestResponse(DomainsResponseEntity())
//              complete(restResponseToHttpResponse(response))
//            }
//          }
//        } ~
//          pathPrefix(Segment) { (domain: String) =>
//            if (!ConfigLiterals.domains.contains(domain))
//              throw UnknownConfigSettingDomain(createMessage("rest.config.setting.domain.unknown", ConfigLiterals.domains.mkString(", ")), domain)
//            pathEndOrSingleSlash {
//              get {
//                val configElements = configService.getByParameters(Map("domain" -> domain))
//                val response = OkRestResponse(ConfigSettingsResponseEntity())
//                if (configElements.nonEmpty) {
//                  response.entity = ConfigSettingsResponseEntity(configElements.map(x => x.asProtocolConfigurationSetting()))
//                }
//
//                complete(restResponseToHttpResponse(response))
//
//              }
//            } ~
//              pathPrefix(Segment) { (name: String) =>
//                pathEndOrSingleSlash {
//                  get {
//                    var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
//                      createMessage("rest.config.setting.notfound", domain, name)))
//                    configService.get(createConfigurationSettingName(domain, name)) match {
//                      case Some(configElement) =>
//                        val entity = ConfigSettingResponseEntity(configElement.asProtocolConfigurationSetting())
//                        response = OkRestResponse(entity)
//                      case None =>
//                    }
//
//                    complete(restResponseToHttpResponse(response))
//                  } ~
//                    delete {
//                      var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
//                        createMessage("rest.config.setting.notfound", domain, name)))
//                      configService.get(createConfigurationSettingName(domain, name)) match {
//                        case Some(_) =>
//                          configService.delete(createConfigurationSettingName(domain, name))
//                          val entity = MessageResponseEntity(createMessage("rest.config.setting.deleted", domain, name))
//                          response = OkRestResponse(entity)
//                        case None =>
//                      }
//
//                      complete(restResponseToHttpResponse(response))
//                    }
//                }
//              }
//          } ~
//          pathEndOrSingleSlash {
//            post { (ctx: RequestContext) =>
//
//
//              var response: Option[RestResponse] = None
//              val errors = new ArrayBuffer[String]
//
//              Try(serializer.deserialize[ConfigurationSettingApi](getEntityFromContext(ctx))) match {
//                case Success(data) =>
//                  errors ++= data.validate()
//
//                  if (errors.isEmpty) {
//                    configService.save(data.asModelConfigurationSetting)
//                    response = Option(
//                      CreatedRestResponse(
//                        MessageResponseEntity(
//                          createMessage("rest.config.setting.created", data.domain, data.name))))
//                  }
//                case Failure(e: JsonDeserializationException) =>
//                  errors += JsonDeserializationErrorMessageCreator(e)
//                case Failure(e) => throw e
//              }
//
//              if (errors.nonEmpty) {
//                response = Option(
//                  BadRequestRestResponse(
//                    MessageResponseEntity(
//                      createMessage("rest.config.setting.cannot.create", errors.mkString("\n")))))
//              }
//
//              ctx.complete(restResponseToHttpResponse(response.get))
//            } ~
//              get {
//                val configElements = configService.getAll
//                val response = OkRestResponse(ConfigSettingsResponseEntity())
//                if (configElements.nonEmpty) {
//                  response.entity = ConfigSettingsResponseEntity(configElements.map(_.asProtocolConfigurationSetting()))
//                }
//
//                complete(restResponseToHttpResponse(response))
//              }
//          }
//      }
//    }


  }
}

