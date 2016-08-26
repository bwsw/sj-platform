package com.bwsw.sj.examples.sflow.module.output.data

import java.util.Date

import com.bwsw.sj.engine.core.entities.EsEntity
import com.fasterxml.jackson.annotation.JsonProperty

/**
  *
  *
  * @author Kseniya Mikhaleva
  */
class TrafficMetrics extends EsEntity {
  var ts: Date = null
  @JsonProperty("src-as") var srcAs: Int= 0
  @JsonProperty("dst-as") var dstAs: String= null
  @JsonProperty("sum-of-traffic") var trafficSum: Long= 0

  override def getDateFields(): Array[String] = {
    val fields = super.getDateFields().toBuffer
    fields.append("ts")
    fields.toArray
  }
}