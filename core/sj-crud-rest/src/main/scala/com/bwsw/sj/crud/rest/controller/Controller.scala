package com.bwsw.sj.crud.rest.controller

import com.bwsw.sj.common.bll.Service
import com.bwsw.sj.common.rest.RestResponse

trait Controller {
  protected val service: Service[_]

  def post(serializedEntity: String): RestResponse

  def getAll(): RestResponse

  def get(name: String): RestResponse

  def delete(name: String): RestResponse
}
