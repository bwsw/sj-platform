package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext, Route}
import com.bwsw.sj.common.rest._
import com.bwsw.sj.crud.rest.SjCrudRestServer
import com.bwsw.sj.crud.rest.controller.ConfigSettingsController
import com.bwsw.sj.crud.rest.utils.CompletionUtils

trait SjConfigurationSettingsRoute extends Directives with SjCrudRestServer with CompletionUtils {
  val configSettingsController = new ConfigSettingsController

  val configSettingsRoute: Route = {
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
            pathEndOrSingleSlash {
              get {
                val response = configSettingsController.getByDomain(domain)
                complete(restResponseToHttpResponse(response))
              }
            } ~
              pathPrefix(Segment) { (name: String) =>
                pathEndOrSingleSlash {
                  get {
                    val response = configSettingsController.get(domain, name)
                    complete(restResponseToHttpResponse(response))
                  } ~
                    delete {
                      val response = configSettingsController.delete(domain, name)
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
