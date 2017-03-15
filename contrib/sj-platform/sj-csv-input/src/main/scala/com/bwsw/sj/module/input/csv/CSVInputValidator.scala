package com.bwsw.sj.module.input.csv

import java.nio.charset.Charset

import com.bwsw.sj.common.engine.StreamingValidator
import com.bwsw.sj.common.utils.ValidationUtils._

/**
  * Validator for csv input module.
  *
  * @author Pavel Tomskikh
  */
class CSVInputValidator extends StreamingValidator {

  override def validate(options: Map[String, Any]) = {
    isRequired(isString)(options, CSVInputOptionNames.lineSeparator) &&
      isRequired(isString)(options, CSVInputOptionNames.encoding) &&
      Charset.isSupported(options(CSVInputOptionNames.encoding).asInstanceOf[String]) &&
      isRequired(isString)(options, CSVInputOptionNames.outputStream) &&
      isRequired(isString)(options, CSVInputOptionNames.fallbackStream) &&
      isOption(isString)(options, CSVInputOptionNames.fieldSeparator) &&
      isOption(isString)(options, CSVInputOptionNames.quoteSymbol) &&
      checkAvroFields(
        options,
        CSVInputOptionNames.fields,
        Seq(CSVInputOptionNames.uniqueKey, CSVInputOptionNames.distribution))
  }
}
