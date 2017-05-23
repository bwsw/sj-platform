package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.ServiceInterface
import com.bwsw.sj.common.rest.RestResponse

trait Controller {
  protected val serializer: JsonSerializer = new JsonSerializer(true, true)
  protected val serviceInterface: ServiceInterface[_,_]

  def create(serializedEntity: String): RestResponse

  def getAll(): RestResponse

  def get(name: String): RestResponse

  def delete(name: String): RestResponse
}
