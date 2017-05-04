package com.bwsw.sj.crud.rest.utils

import com.bwsw.common.exceptions.{JsonDeserializationException, JsonIncorrectValueException, JsonNotParsedException, JsonUnrecognizedPropertyException}
import com.bwsw.sj.common.utils.MessageResourceUtils

/**
  * Creates message for [[com.bwsw.common.exceptions.JsonDeserializationException JsonDeserializationException]].
  *
  * @author Pavel Tomskikh
  */
object JsonDeserializationErrorMessageCreator extends MessageResourceUtils {

  def apply(e: JsonDeserializationException) = {
    createMessage(e match {
      case _: JsonUnrecognizedPropertyException => "json.deserialization.error.unrecognized.property"
      case _: JsonIncorrectValueException => "json.deserialization.error.incorrect.value"
      case _: JsonNotParsedException => "json.deserialization.error.not.parsed"
      case _ => "json.deserialization.error"
    }, e.getMessage)
  }
}
