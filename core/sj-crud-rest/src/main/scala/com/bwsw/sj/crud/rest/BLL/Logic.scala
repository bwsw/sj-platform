package com.bwsw.sj.crud.rest.BLL

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.RestResponse
import com.bwsw.sj.common.utils.MessageResourceUtils

trait Logic[T] extends MessageResourceUtils {
  protected val serializer = new JsonSerializer()
  protected val entityDAO: GenericMongoService[T]

  def process(serializedEntity: String): RestResponse

  def getAll(): RestResponse

  def get(name: String): RestResponse

  def delete(name: String): RestResponse
}
