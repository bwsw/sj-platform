package com.bwsw.sj.crud.rest.api

import java.text.MessageFormat


import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.{BadRequestWithKey}
import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.model.ConfigSetting
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.entities.config.ConfigSettingData
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.config.ConfigSettingValidator
import com.bwsw.sj.crud.rest.utils.ConvertUtil._

/**
 * Rest-api for config file
 *
 */
trait SjConfigSettingsApi extends Directives with SjCrudValidator {

  val configSettingsApi = {
    pathPrefix("config") {
      pathPrefix("settings") {
        pathPrefix(Segment) { (domain: String) =>
          if (!ConfigConstants.domains.contains(domain)) throw new BadRequestWithKey(
            s"Cannot recognize config setting domain. Domain must be one of the following values: ${ConfigConstants.domains.mkString(", ")}",
            s"$domain"
          )
          pathEndOrSingleSlash {
            post { (ctx: RequestContext) =>
              val data = serializer.deserialize[ConfigSettingData](getEntityFromContext(ctx))
              val errors = ConfigSettingValidator.validate(data)
              var response: RestResponse = ConflictRestResponse(
                Map("message" -> MessageFormat.format(messages.getString("rest.config.setting.cannot.create"), errors.mkString("\n"))))
              if (errors.isEmpty) {
                val configElement = new ConfigSetting(
                  domain + "." + data.name,
                  data.value,
                  domain
                )
                configService.save(configElement)
                response = CreatedRestResponse(
                  Map("message" -> MessageFormat.format(messages.getString("rest.config.setting.created"), domain, data.name)))
              }

              ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
            } ~
              get {
                val configElements = configService.getByParameters(Map("domain" -> domain))
                var response: RestResponse = NotFoundRestResponse(
                  Map("message" -> MessageFormat.format(messages.getString("rest.config.settings.domain.notfound"), domain)))
                if (configElements.nonEmpty) {
                  val entity = Map(s"$domain-config-settings" -> configElements.map(x => configSettingToConfigSettingData(x)))
                  response = OkRestResponse(entity)
                }

                complete(HttpEntity(`application/json`, serializer.serialize(response)))

              }
          } ~
            pathPrefix(Segment) { (name: String) =>
              pathEndOrSingleSlash {
                get {
                  var response: RestResponse = NotFoundRestResponse(
                    Map("message" -> MessageFormat.format(messages.getString("rest.config.setting.notfound"), domain, name)))
                  configService.get(domain + "." + name) match {
                    case Some(configElement) =>
                      val entity = Map(s"$domain-config-settings" -> configSettingToConfigSettingData(configElement))
                      response = OkRestResponse(entity)
                    case None =>
                  }

                  complete(HttpEntity(`application/json`, serializer.serialize(response)))
                } ~
                  delete {
                    var response: RestResponse = NotFoundRestResponse(
                      Map("message" -> MessageFormat.format(messages.getString("rest.config.setting.notfound"), domain, name)))
                    configService.get(domain + "." + name) match {
                      case Some(_) =>
                        configService.delete(domain + "." + name)
                        val entity = Map("message" -> MessageFormat.format(messages.getString("rest.config.setting.deleted"), domain, name))
                        response = OkRestResponse(entity)
                      case None =>
                    }

                    complete(HttpEntity(`application/json`, serializer.serialize(response)))
                  }
              }
            }
        } ~ pathEndOrSingleSlash {
          get {
            val configElements = configService.getAll
            var response: RestResponse = OkRestResponse(Map("message" -> messages.getString("rest.config.settings.notfound")))
            if (configElements.nonEmpty) {
              val entity = Map("config-settings" -> configElements.map(x => (x.domain, configSettingToConfigSettingData(x))).toMap)
              response = OkRestResponse(entity)
            }

            complete(HttpEntity(`application/json`, serializer.serialize(response)))
          }
        }
      }
    }
  }
}

