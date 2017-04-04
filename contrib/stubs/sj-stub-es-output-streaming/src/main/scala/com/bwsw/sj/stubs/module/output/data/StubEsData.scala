package com.bwsw.sj.stubs.module.output.data

import java.util.Date

import com.bwsw.sj.engine.core.entities.OutputEnvelope
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * @author Kseniya Tomskikh
  */
class StubEsData extends OutputEnvelope {
  @JsonProperty("test-date") var testDate: Date = null
  var value: Int = 0
  var stringValue = ""

  override def getFieldsValue: Map[String, Any] = {
    Map("test-date" -> testDate, "string-value" -> stringValue, "value" -> value)
  }
}
