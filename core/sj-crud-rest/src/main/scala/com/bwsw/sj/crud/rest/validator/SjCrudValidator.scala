package com.bwsw.sj.crud.rest.validator


import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.RequestContext
import akka.stream.ActorMaterializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.si.JsonValidator
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.crud.rest.utils.CompletionUtils

import scala.concurrent.{Await, ExecutionContextExecutor}

/**
  * Trait for validation of crud-rest-api
  * and contains common methods for routes
  *
  * @author Kseniya Tomskikh
  */
trait SjCrudValidator extends CompletionUtils with JsonValidator {
  val logger: LoggingAdapter

  implicit val system = ActorSystem("sj-crud-rest-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  val serializer: Serializer
  val restHost: String
  val restPort: Int

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

  def validateContextWithSchema(ctx: RequestContext, schema: String) = {
    checkContext(ctx)
    val entity = checkEntity(ctx)
    validateWithSchema(entity, schema)
  }

  private def checkContext(ctx: RequestContext) = {
    if (ctx.request.entity.isKnownEmpty()) {
      throw new Exception(createMessage("rest.errors.empty.entity"))
    }
  }

  private def checkEntity(ctx: RequestContext) = {
    val entity = getEntityFromContext(ctx)
    if (!isJSONValid(entity)) {
      val message = createMessage("rest.errors.entity.invalid.json")
      logger.error(message)
      throw new Exception(message)
    }

    entity
  }
}
