package com.bwsw.sj.stubs.module.output.data

import java.util.Date

import com.bwsw.sj.engine.core.entities.OutputEnvelope
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * @author Kseniya Tomskikh
  */
class StubEsData(
                  @JsonProperty("test-date") val testDate: Date,
                  val value: Int = 0,
                  val stringValue: String = "")
  extends OutputEnvelope {

  override def getFieldsValue: Map[String, Any] = {
    Map("test-date" -> testDate, "string-value" -> stringValue, "value" -> value)
  }
}
