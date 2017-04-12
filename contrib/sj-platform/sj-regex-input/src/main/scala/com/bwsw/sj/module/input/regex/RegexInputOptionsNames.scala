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
case class Field(name: String,
                 defaultValue: String,
                 @JsonProperty(RegexInputOptionsNames.fieldType) _type: String)

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