package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.ServiceInterface
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.result.{Deleted, DeletionError, EntityNotFound}
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import scaldi.Injectable.inject
import scaldi.Injector

trait Controller {
  protected implicit val injector: Injector
  protected val serializer: JsonSerializer = inject[JsonSerializer]
  serializer.disableNullForPrimitives(true)
  protected val jsonDeserializationErrorMessageCreator = inject[JsonDeserializationErrorMessageCreator]
  protected val serviceInterface: ServiceInterface[_, _]

  protected val entityDeletedMessage: String
  protected val entityNotFoundMessage: String

  def create(serializedEntity: String): RestResponse

  def getAll(): RestResponse

  def get(name: String): RestResponse

  def delete(name: String): RestResponse = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.createMessage

    serviceInterface.delete(name) match {
      case Deleted =>
        OkRestResponse(MessageResponseEntity(createMessage(entityDeletedMessage, name)))
      case EntityNotFound =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
      case DeletionError(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }
  }
}
