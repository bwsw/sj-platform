package com.bwsw.sj.examples.pingstation.module.output.data

import java.util.Date

import com.bwsw.sj.engine.core.entities.EsEntity
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 23/06/2016
  *
  * @author Kseniya Mikhaleva
  */
class PingMetrics extends EsEntity {
  var ts: Date = null
  var ip: String = null
  @JsonProperty("avg-time") var avgTime: Double= 0
  @JsonProperty("total-ok") var totalOk: Long= 0
  @JsonProperty("total-unreachable") var totalUnreachable: Long= 0
  var total: Long = 0

  override def getDateFields(): Array[String] = {
    val fields = super.getDateFields().toBuffer
    fields.append("ts")
    fields.toArray
  }
}