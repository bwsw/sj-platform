package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.ServiceInterface
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.result.{Deleted, DeletingError, EntityNotFound}
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

trait Controller {
  protected val serializer: JsonSerializer = new JsonSerializer(true, true)
  protected val serviceInterface: ServiceInterface[_, _]

  protected val entityDeletedMessage: String
  protected val entityNotFoundMessage: String

  def create(serializedEntity: String): RestResponse

  def getAll(): RestResponse

  def get(name: String): RestResponse

  def delete(name: String): RestResponse = {
    serviceInterface.delete(name) match {
      case Deleted =>
        OkRestResponse(MessageResponseEntity(createMessage(entityDeletedMessage, name)))
      case EntityNotFound =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
      case DeletingError(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }
  }
}
