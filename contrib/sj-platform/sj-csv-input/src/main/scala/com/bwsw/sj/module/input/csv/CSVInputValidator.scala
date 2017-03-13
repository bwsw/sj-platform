package com.bwsw.sj.module.input.csv

import java.nio.charset.Charset

import com.bwsw.sj.common.engine.StreamingValidator

/**
  * Validator for csv input module.
  *
  * @author Pavel Tomskikh
  */
class CSVInputValidator extends StreamingValidator {

  val lineSeparator = "line-separator"
  val encoding = "encoding"
  val outputStream = "output-stream"
  val fallbackStream = "fallback-stream"
  val fields = "fields"
  val fieldSeparator = "field-separator"
  val quoteSymbol = "quote-symbol"
  val uniqueKey = "unique-key"

  override def validate(options: Map[String, Any]) = {
    def isRequired(key: String) = {
      val value = options.get(key)
      value.nonEmpty && isString(value.get)
    }

    def isOption(key: String) = {
      val value = options.get(key)
      value.isEmpty || isString(value.get)
    }

    def isString(a: Any) = a.isInstanceOf[String] && a.asInstanceOf[String].nonEmpty

    def checkFields = {
      val fieldsValue = options.get(fields)
      fieldsValue match {
        case Some(fieldNames: Seq[Any]) =>
          fieldNames.nonEmpty && {
            val uniqueKeyValue = options.get(uniqueKey)
            uniqueKeyValue match {
              case Some(uniqueFields: Seq[Any]) =>
                uniqueFields.nonEmpty && uniqueFields.forall(fieldNames.contains)
              case None => true
              case _ => false
            }
          }
        case _ => false
      }
    }

    isRequired(lineSeparator) &&
      isRequired(encoding) &&
      Charset.isSupported(options(encoding).asInstanceOf[String]) &&
      isRequired(outputStream) &&
      isRequired(fallbackStream) &&
      isOption(fieldSeparator) &&
      isOption(quoteSymbol) &&
      checkFields
  }
}
