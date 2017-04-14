package com.bwsw.sj.module.input.csv

import java.nio.charset.Charset

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}
import com.bwsw.sj.common.utils.ValidationUtils._

import scala.collection.mutable.ArrayBuffer

/**
  * Validator for csv input module.
  *
  * @author Pavel Tomskikh
  */
class CSVInputValidator extends StreamingValidator {

  override def validate(options: String): ValidationInfo = {
    val errors = ArrayBuffer[String]()
    val serializer = new JsonSerializer
    val csvInputOptions = serializer.deserialize[CSVInputOptions](options)

    if (!isRequiredStringField(Option(csvInputOptions.lineSeparator)))
      errors += s"'${CSVInputOptionNames.lineSeparator}' attribute is required and should be a non-empty string"
    if (!isRequiredStringField(Option(csvInputOptions.encoding)))
      errors += s"'${CSVInputOptionNames.encoding}' attribute is required and should be a non-empty string"
    else {
      if (!Charset.isSupported(csvInputOptions.encoding))
        errors += s"'${CSVInputOptionNames.encoding}' is not supported"
    }

    if (!isRequiredStringField(Option(csvInputOptions.outputStream)))
      errors += s"'${CSVInputOptionNames.outputStream}' attribute is required and should be a non-empty string"
    if (!isRequiredStringField(Option(csvInputOptions.fallbackStream)))
      errors += s"'${CSVInputOptionNames.fallbackStream}' attribute is required and should be a non-empty string"

    if (!isOptionStringField(csvInputOptions.fieldSeparator))
      errors += s"'${CSVInputOptionNames.fieldSeparator}' should be a non-empty string"

    if (!isOptionStringField(csvInputOptions.quoteSymbol))
      errors += s"'${CSVInputOptionNames.quoteSymbol}' should be a non-empty string"

    if (!checkFields(
      Option(csvInputOptions.fields),
      Option(csvInputOptions.uniqueKey),
      Option(csvInputOptions.distribution)))
      errors += s"'${CSVInputOptionNames.fields}' hasn't passed validation"

    ValidationInfo(errors.isEmpty, errors)
  }
}