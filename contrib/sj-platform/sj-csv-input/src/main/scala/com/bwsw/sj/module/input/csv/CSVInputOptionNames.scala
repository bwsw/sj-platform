package com.bwsw.sj.module.input.csv

/**
  *
  * @author Pavel Tomskikh
  */
object CSVInputOptionNames {

  val lineSeparator = "lineSeparator"
  val encoding = "encoding"
  val outputStream = "outputStream"
  val fallbackStream = "fallbackStream"
  val fields = "fields"
  val fieldSeparator = "fieldSeparator"
  val quoteSymbol = "quoteSymbol"
}

case class CSVInputOptions(lineSeparator: String,
                           encoding: String,
                           outputStream: String,
                           fallbackStream: String,
                           fields: List[String],
                           fieldSeparator: Option[String],
                           quoteSymbol: Option[String],
                           var uniqueKey: List[String],
                           distribution: List[String])