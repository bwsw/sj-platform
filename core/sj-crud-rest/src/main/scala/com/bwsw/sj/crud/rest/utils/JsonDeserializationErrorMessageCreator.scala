package com.bwsw.sj.crud.rest.utils

import com.bwsw.common.exceptions._
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Creates message for [[com.bwsw.common.exceptions.JsonDeserializationException JsonDeserializationException]].
  *
  * @author Pavel Tomskikh
  */
object JsonDeserializationErrorMessageCreator {

  def apply(exception: JsonDeserializationException,
            attributeRequiredMessage: String = "entity.error.attribute.required")
           (implicit injector: Injector): String = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.createMessage

    createMessage(exception match {
      case _: JsonUnrecognizedPropertyException => "json.deserialization.error.unrecognized.property"
      case _: JsonIncorrectValueException => "json.deserialization.error.incorrect.value"
      case _: JsonNotParsedException => "json.deserialization.error.not.parsed"
      case _: JsonMissedPropertyException => attributeRequiredMessage
      case _ => "json.deserialization.error"
    }, exception.getMessage)
  }
}
