package com.bwsw.sj.examples.sflow.module.output.data

import java.util.Date

import com.bwsw.sj.engine.core.entities.OutputEnvelope
import com.fasterxml.jackson.annotation.JsonProperty

/**
  *
  *
  * @author Kseniya Mikhaleva
  */
class TrafficMetrics extends OutputEnvelope {
  var ts: Date = null
  @JsonProperty("src-as") var srcAs: Int= 0
  @JsonProperty("dst-as") var dstAs: String= null
  @JsonProperty("sum-of-traffic") var trafficSum: Long= 0

  def getMapFields: Map[String, Any] = {
    Map(
      "ts" -> ts,
      "src-as" -> srcAs,
      "dst-as" -> dstAs,
      "sum-of-traffic" -> trafficSum
    )
  }
}