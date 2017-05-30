package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext, Route}
import com.bwsw.sj.crud.rest.SjCrudRestServer
import com.bwsw.sj.crud.rest.controller.ProviderController
import com.bwsw.sj.crud.rest.utils.CompletionUtils

trait SjProvidersRoute extends Directives with SjCrudRestServer with CompletionUtils {
  private val providerController = new ProviderController()

  val providersRoute: Route = {
    pathPrefix("providers") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val entity = getEntityFromContext(ctx)
          val response = providerController.create(entity)

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            complete(restResponseToHttpResponse(providerController.getAll()))
          }
      } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(providerController.getTypes()))
            }
          }
        } ~
        pathPrefix(Segment) { (providerName: String) =>
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(providerController.get(providerName)))
            } ~
              delete {
                complete(restResponseToHttpResponse(providerController.delete(providerName)))
              }
          } ~
            pathPrefix("connection") {
              pathEndOrSingleSlash {
                get {
                  complete(restResponseToHttpResponse(providerController.checkConnection(providerName)))
                }
              }
            } ~
            pathPrefix("related") {
              pathEndOrSingleSlash {
                get {
                  complete(restResponseToHttpResponse(providerController.getRelated(providerName)))
                }
              }
            }
        }
    }
  }
}
