package com.bwsw.sj.engine.core.entities

/**
 * Provides a common data type for an EsEnvelope data field
 * You should define such fields (class variables) that will be in congruence with an ES index document structure
 */

class OutputData extends Serializable {
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
