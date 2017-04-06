package com.bwsw.sj.module.input.regex

import java.nio.charset.Charset
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.engine.StreamingValidator
import com.bwsw.sj.common.utils.ValidationUtils._

/**
  * Implementation of Streaming Validator for Regex Input module
  *
  * @author Ruslan Komarov
  */
class RegexInputValidator extends StreamingValidator {
  /**
    * Validates options' fields of Regex Input module
    *
    * @param options Option parameters
    * @return The result of the validation
    */
  override def validate(options: Map[String, Any]): Boolean = {
    val isRequiredString = isRequired(isString)(_,_)
    def isRequiredStringForValue(value: Any) = isString(value)

    def validateField(field: Field) =
      isRequiredStringForValue(field._type) &&
      isRequiredStringForValue(field.defaultValue) &&
      isRequiredStringForValue(field.name)

    def validateRule(rule: Rule) =
      isRequiredStringForValue(rule.regex) &&
        isRequiredStringForValue(rule.outputStream) &&
        rule.fields.nonEmpty &&
        rule.fields.forall(validateField) &&
        rule.distribution.forall(rule.fields.map(_.name).contains) &&
        rule.distribution.forall(rule.fields.map(_.name).contains)

    val serializer = new JsonSerializer
    val rules = options(RegexInputOptionsNames.rules).asInstanceOf[List[Any]]
      .map(serializer.serialize)
      .map(serializer.deserialize[Rule])

    isRequiredString(options, RegexInputOptionsNames.lineSeparator) &&
      isRequiredString(options, RegexInputOptionsNames.policy) &&
      Charset.isSupported(options(RegexInputOptionsNames.encoding).asInstanceOf[String]) &&
      isRequiredString(options, RegexInputOptionsNames.fallbackStream) &&
      rules.nonEmpty &&
      rules.forall(validateRule)
  }
}
