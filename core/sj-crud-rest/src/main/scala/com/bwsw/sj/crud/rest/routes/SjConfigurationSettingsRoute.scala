package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.crud.rest.controller.ConfigSettingsController
import com.bwsw.sj.crud.rest.exceptions.UnknownConfigSettingDomain
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

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
  }
}

