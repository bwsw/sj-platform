package com.bwsw.sj.crud.rest.utils

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.rest.entities.RestResponse

/**
 * Provides methods for completion of sj-api response
 */
trait CompletionUtils {
  private val responseSerializer = new JsonSerializer()

  def restResponseToHttpResponse(restResponse: RestResponse) = {
    HttpResponse(
      status = restResponse.statusCode,
      entity = HttpEntity(`application/json`, responseSerializer.serialize(restResponse))
    )
  }
}
