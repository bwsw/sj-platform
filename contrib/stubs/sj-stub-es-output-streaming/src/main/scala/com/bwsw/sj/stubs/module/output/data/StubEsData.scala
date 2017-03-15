package com.bwsw.sj.stubs.module.output.data

import java.util.Date

import com.bwsw.sj.engine.core.entities.{EsEnvelope, OutputEnvelope}
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * @author Kseniya Tomskikh
  */
class StubEsData extends OutputEnvelope {
  @JsonProperty("test-date") var testDate: Date = null
  var value: Int = 0

  override def getMapFields: Map[String, AnyRef] = {
    Map("test-date" -> testDate, "value" -> value)
  }
}
