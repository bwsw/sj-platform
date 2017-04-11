package com.bwsw.sj.module.input.regex

import java.nio.charset.Charset

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}
import com.bwsw.sj.common.utils.ValidationUtils._

import scala.collection.mutable.ArrayBuffer

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
  override def validate(options: Map[String, Any]): ValidationInfo = {
    val isRequiredString = isRequired(isString)(_, _)

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

    val errors = ArrayBuffer[String]()
    val serializer = new JsonSerializer
    val rules = options(RegexInputOptionsNames.rules).asInstanceOf[List[Any]]
      .map(serializer.serialize)
      .map(serializer.deserialize[Rule])

    if (!isRequiredString(options, RegexInputOptionsNames.lineSeparator))
      errors += s"'${RegexInputOptionsNames.lineSeparator}' attribute is required and should be a non-empty string"
    if (!isRequiredString(options, RegexInputOptionsNames.policy))
      errors += s"'${RegexInputOptionsNames.policy}' attribute is required and should be a non-empty string"

    if (!Charset.isSupported(options(RegexInputOptionsNames.encoding).asInstanceOf[String]))
      errors += s"'${RegexInputOptionsNames.encoding}' is not supported"

    if (!isRequiredString(options, RegexInputOptionsNames.fallbackStream))
      errors += s"'${RegexInputOptionsNames.fallbackStream}' attribute is required and should be a non-empty string"

    if (rules.isEmpty)
      errors += s"'${RegexInputOptionsNames.rules}' attribute is required and should be a non-empty set"
    if (!rules.forall(validateRule))
      errors += s"'${RegexInputOptionsNames.rules}' hasn't passed validation"

    ValidationInfo(errors.isEmpty, errors)
  }
}
