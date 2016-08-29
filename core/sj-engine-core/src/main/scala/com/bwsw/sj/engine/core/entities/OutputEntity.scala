package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonProperty

class OutputEntity extends Serializable {

  @JsonProperty("output-date-time") var outputDateTime: String = null
  @JsonProperty("txn-date-time") var txnDateTime: String = null
  var txn: String = null
  var stream: String = null
  var partition: Int = 0

  def getDateFields(): Array[String] = {
    Array("output-date-time", "txn-date-time")
  }

}
