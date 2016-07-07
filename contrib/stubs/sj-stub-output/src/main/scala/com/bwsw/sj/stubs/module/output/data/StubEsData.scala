package com.bwsw.sj.stubs.module.output.data

import java.util.Date

import com.bwsw.sj.engine.core.entities.EsEntity
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 27/05/2016
  *
  * @author Kseniya Tomskikh
  */
class StubEsData extends EsEntity {
  @JsonProperty("test-date") var testDate: Date = null
  var value: Int = 0

  override def getDateFields(): Array[String] = {
    val fields = super.getDateFields().toBuffer
    fields.append("test-date")
    fields.toArray
  }
}
