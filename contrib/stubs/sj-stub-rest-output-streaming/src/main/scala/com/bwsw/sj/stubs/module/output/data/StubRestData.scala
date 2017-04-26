package com.bwsw.sj.stubs.module.output.data

import com.bwsw.sj.engine.core.entities.OutputEnvelope

/**
  * @author Pavel Tomskikh
  */
class StubRestData extends OutputEnvelope {

  var value: Int = 0
  var stringValue: String = ""

  override def getFieldsValue: Map[String, Any] = {
    Map(
      "value" -> value,
      "stringValue" -> stringValue)
  }
}
