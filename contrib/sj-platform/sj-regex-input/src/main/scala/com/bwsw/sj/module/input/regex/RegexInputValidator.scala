/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
  override def validate(options: String): ValidationInfo = {
    def validateField(field: Field) =
      isRequiredStringField(Option(field._type)) &&
        isRequiredStringField(Option(field.defaultValue)) &&
        isRequiredStringField(Option(field.name))

    def validateRule(rule: Rule) =
      isRequiredStringField(Option(rule.regex)) &&
        isRequiredStringField(Option(rule.outputStream)) &&
        rule.fields.nonEmpty &&
        rule.fields.forall(validateField) &&
        rule.uniqueKey.forall(rule.fields.map(_.name).contains) &&
        rule.distribution.forall(rule.fields.map(_.name).contains)

    val errors = ArrayBuffer[String]()
    val serializer = new JsonSerializer
    val regexInputOptions = serializer.deserialize[RegexInputOptions](options)

    if (!isRequiredStringField(Option(regexInputOptions.lineSeparator)))
      errors += s"'${RegexInputOptionsNames.lineSeparator}' attribute is required and should be a non-empty string"
    if (!isRequiredStringField(Option(regexInputOptions.policy)))
      errors += s"'${RegexInputOptionsNames.policy}' attribute is required and should be a non-empty string"

    if (!isRequiredStringField(Option(regexInputOptions.encoding)))
      errors += s"'${RegexInputOptionsNames.encoding}' attribute is required and should be a non-empty string"
    else {
      if (!Charset.isSupported(regexInputOptions.encoding))
        errors += s"'${RegexInputOptionsNames.encoding}' is not supported"
    }

    if (!isRequiredStringField(Option(regexInputOptions.fallbackStream)))
      errors += s"'${RegexInputOptionsNames.fallbackStream}' attribute is required and should be a non-empty string"

    if (regexInputOptions.rules.isEmpty)
      errors += s"'${RegexInputOptionsNames.rules}' attribute is required and should be a non-empty set"
    if (!regexInputOptions.rules.forall(validateRule))
      errors += s"'${RegexInputOptionsNames.rules}' hasn't passed validation"

    ValidationInfo(errors.isEmpty, errors)
  }
}