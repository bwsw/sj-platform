package com.bwsw.sj.engine.core.entities

/**
 * Provides a common data type for EsEnvelope data field
 */

class OutputData extends Serializable {
  def getDateFields(): Array[String] = {
    Array("output-date-time", "txn-date-time")
  }
}
