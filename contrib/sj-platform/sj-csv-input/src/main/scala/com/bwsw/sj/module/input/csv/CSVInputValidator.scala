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