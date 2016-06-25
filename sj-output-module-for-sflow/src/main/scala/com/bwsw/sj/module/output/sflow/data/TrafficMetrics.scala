package com.bwsw.sj.module.output.sflow.data

import com.bwsw.sj.engine.core.entities.EsEntity
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 23/06/2016
  *
  * @author Kseniya Mikhaleva
  */
class TrafficMetrics extends EsEntity {
  var ts: Long = 0
  @JsonProperty("src-as") var srcAs: Int= 0
  @JsonProperty("dst-as") var dstAs: Int= 0
  @JsonProperty("sum-of-traffic") var trafficSum: Long= 0
}
