package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.config.ConfigurationSettingData
import com.bwsw.sj.crud.rest.exceptions.UnknownConfigSettingDomain
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.collection.mutable

trait SjConfigurationSettingsApi extends Directives with SjCrudValidator {

  val configSettingsApi = {
    pathPrefix("config") {
      pathPrefix("settings") {
        pathPrefix(Segment) { (domain: String) =>
          if (!ConfigLiterals.domains.contains(domain))
            throw UnknownConfigSettingDomain(createMessage("rest.config.setting.domain.unknown", ConfigLiterals.domains.mkString(", ")), domain)
          pathEndOrSingleSlash {
            post { (ctx: RequestContext) =>
              checkContext(ctx)
              val data = serializer.deserialize[ConfigurationSettingData](getEntityFromContext(ctx))
              val errors = data.validate(domain)
              var response: RestResponse = BadRequestRestResponse(Map("message" ->
                createMessage("rest.config.setting.cannot.create", errors.mkString("\n"))))
              if (errors.isEmpty) {
                configService.save(data.asModelConfigurationSetting(domain))
                response = CreatedRestResponse(Map("message" ->
                  createMessage("rest.config.setting.created", domain, data.name)))
              }

              ctx.complete(restResponseToHttpResponse(response))
            } ~
              get {
                val configElements = configService.getByParameters(Map("domain" -> domain))
                val response = OkRestResponse(Map(s"$domain-config-settings" -> mutable.Buffer()))
                if (configElements.nonEmpty) {
                  response.entity = Map(s"$domain-config-settings" -> configElements.map(x => x.asProtocolConfigurationSetting()))
                }

                complete(restResponseToHttpResponse(response))

              }
          } ~
            pathPrefix(Segment) { (name: String) =>
              pathEndOrSingleSlash {
                get {
                  var response: RestResponse = NotFoundRestResponse(Map("message" ->
                    createMessage("rest.config.setting.notfound", domain, name)))
                  configService.get(createConfigurationSettingName(domain, name)) match {
                    case Some(configElement) =>
                      val entity = Map(s"$domain-config-settings" -> configElement.asProtocolConfigurationSetting())
                      response = OkRestResponse(entity)
                    case None =>
                  }

                  complete(restResponseToHttpResponse(response))
                } ~
                  delete {
                    var response: RestResponse = NotFoundRestResponse(Map("message" ->
                      createMessage("rest.config.setting.notfound", domain, name)))
                    configService.get(createConfigurationSettingName(domain, name)) match {
                      case Some(_) =>
                        configService.delete(createConfigurationSettingName(domain, name))
                        val entity = Map("message" -> createMessage("rest.config.setting.deleted", domain, name))
                        response = OkRestResponse(entity)
                      case None =>
                    }

                    complete(restResponseToHttpResponse(response))
                  }
              }
            }
        } ~ pathEndOrSingleSlash {
          get {
            val configElements = configService.getAll
            val response = OkRestResponse(Map(s"config-settings" -> mutable.Buffer()))
            if (configElements.nonEmpty) {
              response.entity = Map("config-settings" -> configElements.map(x => (x.domain, x.asProtocolConfigurationSetting())).toMap)
            }

            complete(restResponseToHttpResponse(response))
          }
        }
      }
    }
  }
}

