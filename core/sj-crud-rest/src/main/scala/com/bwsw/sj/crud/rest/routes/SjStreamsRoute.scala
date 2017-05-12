package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.crud.rest.controller.StreamController
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

trait SjStreamsRoute extends Directives with SjCrudValidator {

  private val streamController = new StreamController

  val streamsRoute = {
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
