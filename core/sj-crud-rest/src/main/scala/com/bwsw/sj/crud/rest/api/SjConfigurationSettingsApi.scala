package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest.DTO._
import com.bwsw.sj.common.rest.DTO.config.ConfigurationSettingData
import com.bwsw.sj.common.rest._
import com.bwsw.sj.crud.rest.exceptions.UnknownConfigSettingDomain
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.collection.mutable.ArrayBuffer

trait SjConfigurationSettingsApi extends Directives with SjCrudValidator {

  val configSettingsApi = {
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
              throw UnknownConfigSettingDomain(createMessage("rest.config.setting.domain.unknown", ConfigLiterals.domains.mkString(", ")), domain)
            pathEndOrSingleSlash {
              get {
                val configElements = configService.getByParameters(Map("domain" -> domain))
                val response = OkRestResponse(ConfigSettingsResponseEntity())
                if (configElements.nonEmpty) {
                  response.entity = ConfigSettingsResponseEntity(configElements.map(x => x.asProtocolConfigurationSetting()))
                }

                complete(restResponseToHttpResponse(response))

              }
            } ~
              pathPrefix(Segment) { (name: String) =>
                pathEndOrSingleSlash {
                  get {
                    var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
                      createMessage("rest.config.setting.notfound", domain, name)))
                    configService.get(createConfigurationSettingName(domain, name)) match {
                      case Some(configElement) =>
                        val entity = ConfigSettingResponseEntity(configElement.asProtocolConfigurationSetting())
                        response = OkRestResponse(entity)
                      case None =>
                    }

                    complete(restResponseToHttpResponse(response))
                  } ~
                    delete {
                      var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
                        createMessage("rest.config.setting.notfound", domain, name)))
                      configService.get(createConfigurationSettingName(domain, name)) match {
                        case Some(_) =>
                          configService.delete(createConfigurationSettingName(domain, name))
                          val entity = MessageResponseEntity(createMessage("rest.config.setting.deleted", domain, name))
                          response = OkRestResponse(entity)
                        case None =>
                      }

                      complete(restResponseToHttpResponse(response))
                    }
                }
              }
          } ~
          pathEndOrSingleSlash {
            post { (ctx: RequestContext) =>
              var response: RestResponse = null
              val errors = new ArrayBuffer[String]

              try {
                val data = serializer.deserialize[ConfigurationSettingData](getEntityFromContext(ctx))
                errors ++= data.validate()

                if (errors.isEmpty) {
                  configService.save(data.asModelConfigurationSetting)
                  response = CreatedRestResponse(MessageResponseEntity(createMessage("rest.config.setting.created", data.domain, data.name)))
                }
              } catch {
                case e: JsonDeserializationException =>
                  errors += JsonDeserializationErrorMessageCreator(e)
              }

              if (errors.nonEmpty) {
                response = BadRequestRestResponse(MessageResponseEntity(
                  createMessage("rest.config.setting.cannot.create", errors.mkString("\n"))
                ))
              }

              ctx.complete(restResponseToHttpResponse(response))
            } ~
              get {
                val configElements = configService.getAll
                val response = OkRestResponse(ConfigSettingsResponseEntity())
                if (configElements.nonEmpty) {
                  response.entity = ConfigSettingsResponseEntity(configElements.map(_.asProtocolConfigurationSetting()))
                }

                complete(restResponseToHttpResponse(response))
              }
          }
      }
    }
  }
}

