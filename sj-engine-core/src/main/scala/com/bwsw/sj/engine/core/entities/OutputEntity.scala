package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 27/05/2016
  *
  * @author Kseniya Tomskikh
  */
class OutputEntity extends Serializable {

  @JsonProperty("txn-date-time") var txnDateTime: String = null
  var txn: String = null
  var stream: String = null
  var partition: Int = 0

}
