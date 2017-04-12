package com.bwsw.sj.module.input.csv

import java.nio.charset.Charset

import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}
import com.bwsw.sj.common.utils.ValidationUtils._

import scala.collection.mutable.ArrayBuffer

/**
  * Validator for csv input module.
  *
  * @author Pavel Tomskikh
  */
class CSVInputValidator extends StreamingValidator {

  override def validate(options: Map[String, Any]): ValidationInfo = {
    val errors = ArrayBuffer[String]()
    if (!isRequired(isString)(options, CSVInputOptionNames.lineSeparator))
      errors += s"'${CSVInputOptionNames.lineSeparator}' attribute is required and should be a non-empty string"
    if (!isRequired(isString)(options, CSVInputOptionNames.encoding))
      errors += s"'${CSVInputOptionNames.encoding}' attribute is required and should be a non-empty string"

    if (!Charset.isSupported(options(CSVInputOptionNames.encoding).asInstanceOf[String]))
      errors += s"'${CSVInputOptionNames.encoding}' is not supported"

    if (!isRequired(isString)(options, CSVInputOptionNames.outputStream))
      errors += s"'${CSVInputOptionNames.outputStream}' attribute is required and should be a non-empty string"
    if (!isRequired(isString)(options, CSVInputOptionNames.fallbackStream))
      errors += s"'${CSVInputOptionNames.fallbackStream}' attribute is required and should be a non-empty string"

    if (!isOption(isString)(options, CSVInputOptionNames.fieldSeparator))
      errors += s"'${CSVInputOptionNames.fieldSeparator}' should be a non-empty string"

    if (!isOption(isString)(options, CSVInputOptionNames.quoteSymbol))
      errors += s"'${CSVInputOptionNames.fieldSeparator}' should be a non-empty string"

    if (!checkAvroFields(
      options,
      CSVInputOptionNames.fields,
      Seq(CSVInputOptionNames.uniqueKey, CSVInputOptionNames.distribution)))
      errors += s"'${CSVInputOptionNames.fields}' hasn't passed validation"

    ValidationInfo(errors.isEmpty, errors)
  }
}
