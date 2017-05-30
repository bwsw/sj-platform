package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext, Route}
import com.bwsw.sj.common.SjInjector
import com.bwsw.sj.crud.rest.SjCrudRestServer
import com.bwsw.sj.crud.rest.controller.ServiceController
import com.bwsw.sj.crud.rest.utils.CompletionUtils

trait SjServicesRoute extends Directives with SjCrudRestServer with CompletionUtils with SjInjector {
  private val serviceController = new ServiceController()

  val servicesRoute: Route = {
    pathPrefix("services") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val entity = getEntityFromContext(ctx)
          val response = serviceController.create(entity)

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            complete(restResponseToHttpResponse(serviceController.getAll()))
          }
      } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(serviceController.getTypes()))
            }
          }
        } ~
        pathPrefix(Segment) { (serviceName: String) =>
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(serviceController.get(serviceName)))
            } ~
              delete {
                complete(restResponseToHttpResponse(serviceController.delete(serviceName)))
              }
          } ~
            pathPrefix("related") {
              pathEndOrSingleSlash {
                get {
                  complete(restResponseToHttpResponse(serviceController.getRelated(serviceName)))
                }
              }
            }
        }
    }
  }
}
