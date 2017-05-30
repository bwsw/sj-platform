package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext, Route}
import com.bwsw.sj.crud.rest.SjCrudRestServer
import com.bwsw.sj.crud.rest.controller.StreamController
import com.bwsw.sj.crud.rest.utils.CompletionUtils

trait SjStreamsRoute extends Directives with SjCrudRestServer with CompletionUtils {

  private val streamController = new StreamController

  val streamsRoute: Route = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val entity = getEntityFromContext(ctx)
          ctx.complete(restResponseToHttpResponse(streamController.create(entity)))
        } ~
          get {
            complete(restResponseToHttpResponse(streamController.getAll()))
          }
      } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(streamController.getTypes))
            }
          }
        } ~
        pathPrefix(Segment) { (streamName: String) =>
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(streamController.get(streamName)))
            } ~
              delete {
                complete(restResponseToHttpResponse(streamController.delete(streamName)))
              }
          } ~
            pathPrefix("related") {
              pathEndOrSingleSlash {
                get {
                  complete(restResponseToHttpResponse(streamController.getRelated(streamName)))
                }
              }
            }
        }
    }
  }
}
