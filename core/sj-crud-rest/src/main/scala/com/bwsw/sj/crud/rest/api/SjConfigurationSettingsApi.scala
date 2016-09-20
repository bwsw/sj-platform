package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.config.ConfigurationSettingData
import com.bwsw.sj.common.utils.ConfigLiterals
import com.bwsw.sj.crud.rest.exceptions.UnknownConfigSettingDomain
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

trait SjConfigurationSettingsApi extends Directives with SjCrudValidator with CompletionUtils {

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
              val errors = data.validate()
              var response: RestResponse = ConflictRestResponse(Map("message" ->
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
                var response: RestResponse = NotFoundRestResponse(Map("message" ->
                  createMessage("rest.config.settings.domain.notfound", domain)))
                if (configElements.nonEmpty) {
                  val entity = Map(s"$domain-config-settings" -> configElements.map(x => x.asProtocolConfigurationSetting()))
                  response = OkRestResponse(entity)
                }

                complete(restResponseToHttpResponse(response))

              }
          } ~
            pathPrefix(Segment) { (name: String) =>
              pathEndOrSingleSlash {
                get {
                  var response: RestResponse = NotFoundRestResponse(Map("message" ->
                    createMessage("rest.config.setting.notfound", domain, name)))
                  configService.get(domain + "." + name) match {
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
                    configService.get(domain + "." + name) match {
                      case Some(_) =>
                        configService.delete(domain + "." + name)
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
            var response: RestResponse = OkRestResponse(Map("message" -> getMessage("rest.config.settings.notfound")))
            if (configElements.nonEmpty) {
              val entity = Map("config-settings" -> configElements.map(x => (x.domain, x.asProtocolConfigurationSetting())).toMap)
              response = OkRestResponse(entity)
            }

            complete(restResponseToHttpResponse(response))
          }
        }
      }
    }
  }
}

