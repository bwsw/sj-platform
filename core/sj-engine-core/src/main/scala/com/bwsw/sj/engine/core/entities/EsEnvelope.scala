package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a message that is received from an OutputExecutor
 *
 * Provides a wrapper for elasticsearch entity.
 */

class EsEnvelope() extends Envelope {
  streamType = "elasticsearch-output"
  var data: OutputData = null
  @JsonProperty("output-date-time") var outputDateTime: String = null
  @JsonProperty("txn-date-time") var txnDateTime: String = null
  var txn: String = null
}

