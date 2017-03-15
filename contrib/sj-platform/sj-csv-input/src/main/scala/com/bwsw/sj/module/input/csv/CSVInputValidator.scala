package com.bwsw.sj.module.input.csv

import java.nio.charset.Charset

import com.bwsw.sj.common.engine.StreamingValidator

/**
  * Validator for csv input module.
  *
  * @author Pavel Tomskikh
  */
class CSVInputValidator extends StreamingValidator {

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
      val fieldsValue = options.get(CSVInputOptionNames.fields)
      fieldsValue match {
        case Some(fieldNames: Seq[Any]) =>
          fieldNames.nonEmpty && {
            val uniqueKeyValue = options.get(CSVInputOptionNames.uniqueKey)
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

    isRequired(CSVInputOptionNames.lineSeparator) &&
      isRequired(CSVInputOptionNames.encoding) &&
      Charset.isSupported(options(CSVInputOptionNames.encoding).asInstanceOf[String]) &&
      isRequired(CSVInputOptionNames.outputStream) &&
      isRequired(CSVInputOptionNames.fallbackStream) &&
      isOption(CSVInputOptionNames.fieldSeparator) &&
      isOption(CSVInputOptionNames.quoteSymbol) &&
      checkFields
  }
}
