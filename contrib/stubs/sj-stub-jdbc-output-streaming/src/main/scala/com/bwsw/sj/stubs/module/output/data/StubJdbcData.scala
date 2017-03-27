package com.bwsw.sj.stubs.module.output.data

import com.bwsw.sj.engine.core.entities.OutputEnvelope

/**
  * @author Diryavkin Dmitry
  */

class StubJdbcData extends OutputEnvelope {
  var value: Int = 0
  var id: String = java.util.UUID.randomUUID.toString

  override def getFieldsValue: Map[String, Any] = {
    Map("value" -> value, "id" -> id)
  }
}