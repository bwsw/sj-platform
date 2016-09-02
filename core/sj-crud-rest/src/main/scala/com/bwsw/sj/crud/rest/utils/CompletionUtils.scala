package com.bwsw.sj.crud.rest.utils

import java.text.MessageFormat
import java.util.ResourceBundle

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.crud.rest.entities.RestResponse

/**
 * Provides methods for completion of sj-api response
 */
trait CompletionUtils {

  private val responseSerializer = new JsonSerializer()
  private val messages = ResourceBundle.getBundle("messages")

  def restResponseToHttpResponse(restResponse: RestResponse) = {
    HttpResponse(
      status = restResponse.statusCode,
      entity = HttpEntity(`application/json`, responseSerializer.serialize(restResponse))
    )
  }

  def createMessage(name: String, params: String*) = {
    MessageFormat.format(getMessage(name), params: _*)
  }

  def getMessage(name: String) = {
    messages.getString(name)
  }
}
