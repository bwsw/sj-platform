package com.bwsw.sj.crud.rest.controller

import com.bwsw.sj.common.service.Service
import com.bwsw.sj.common.rest.RestResponse

trait Controller {
  protected val service: Service[_]

  def create(serializedEntity: String): RestResponse

  def getAll(): RestResponse

  def get(name: String): RestResponse

  def delete(name: String): RestResponse
}
