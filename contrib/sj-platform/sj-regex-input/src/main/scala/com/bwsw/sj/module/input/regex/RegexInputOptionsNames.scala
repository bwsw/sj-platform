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

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Constants for options names and possible values
  */
object RegexInputOptionsNames {
  final val lineSeparator = "lineSeparator"
  final val policy = "policy"
  final val rules = "rules"

  final val fieldType = "type"

  final val encoding = "encoding"
  final val fallbackStream = "fallbackStream"

  final val firstMatchWinPolicy = "first-match-win"
  final val checkEveryPolicy = "check-every"

  final val outputRecordName = "regex"
  final val outputFieldName = "data"

  final val fallbackRecordName = "fallback"
  final val fallbackFieldName = "data"
}

case class RegexInputOptions(lineSeparator: String, policy: String, encoding: String, fallbackStream: String, rules: List[Rule])

/**
  * Model class for list of fields in Rule
  *
  * @param name         Field's name
  * @param defaultValue Field's default value
  * @param _type        Field's type
  */
case class Field(@JsonProperty("name") name: String,
                 @JsonProperty("defaultValue") defaultValue: String,
                 @JsonProperty(RegexInputOptionsNames.fieldType) _type: String)

object Field {
  val convertType = Map(
    "boolean" -> ((s: String) => s.toBoolean),
    "int" -> ((s: String) => s.toInt),
    "long" -> ((s: String) => s.toLong),
    "float" -> ((s: String) => s.toFloat),
    "double" -> ((s: String) => s.toDouble),
    "string" -> ((s: String) => s))
}

/**
  * Model class for list of rules in options
  *
  * @param regex        Regular expression used for filtering received data
  * @param fields       Fields for output Avro record
  * @param outputStream Name of output stream
  * @param uniqueKey    Sublist of fields that used to check uniqueness of record
  * @param distribution Sublist of fields that used to compute the output partition number
  */
case class Rule(regex: String,
                fields: List[Field],
                outputStream: String,
                uniqueKey: List[String],
                distribution: List[String])