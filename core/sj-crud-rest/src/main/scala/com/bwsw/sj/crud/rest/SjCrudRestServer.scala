package com.bwsw.sj.crud.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.RequestContext
import akka.stream.ActorMaterializer

import scala.concurrent.{Await, ExecutionContextExecutor}

/**
  * Trait for handling the actor system of rest.
  * Also it provides a method for extracting entity from HTTP-request
  *
  * @author Kseniya Tomskikh
  */
trait SjCrudRestServer {
  implicit val system = ActorSystem("sj-crud-rest-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  /**
    * Getting entity from HTTP-request
    *
    * @param ctx - request context
    * @return - entity from http-request as string
    */
  def getEntityFromContext(ctx: RequestContext): String = {
    getEntityAsString(ctx.request.entity)
  }

  private def getEntityAsString(entity: HttpEntity): String = {
    import scala.concurrent.duration._
    Await.result(entity.toStrict(1.second), 1.seconds).data.decodeString("UTF-8")
  }
}
