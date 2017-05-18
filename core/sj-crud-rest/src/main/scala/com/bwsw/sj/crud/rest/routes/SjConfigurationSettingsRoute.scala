package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.crud.rest.controller.ConfigSettingsController
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
            configSettingsController.checkDomain(domain)

            pathEndOrSingleSlash {
              get {
                val response = configSettingsController.getDomain(domain)
                complete(restResponseToHttpResponse(response))
              }
            } ~
              pathPrefix(Segment) { (name: String) =>
                pathEndOrSingleSlash {
                  get {
                    val response = configSettingsController.get(ConfigurationSetting.createConfigurationSettingName(domain, name))
                    complete(restResponseToHttpResponse(response))
                  } ~
                    delete {
                      val response = configSettingsController.delete(ConfigurationSetting.createConfigurationSettingName(domain, name))
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

