package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a message that is received from an OutputExecutor
 *
 * Provides a wrapper for elasticsearch entity.
 */

class EsEnvelope() extends Envelope {
  streamType = "elasticsearch-output"
  @JsonProperty("output-date-time") var outputDateTime: String = null
  @JsonProperty("txn-date-time") var transactionDateTime: String = null

  /**
   * Allows adding the field(s) of Date type to a standard set of fields
   * (that contains 'output-date-time' and 'txn-date-time' fields by default)
   * that is used to define in the mapping which fields contain dates
   * (usually this field represents the time that events occurred)
   */
  def getDateFields(): Array[String] = {
    Array("output-date-time", "txn-date-time")
  }
}

