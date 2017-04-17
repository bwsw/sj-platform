package com.bwsw.sj.stubs.module.output.data

import com.bwsw.sj.engine.core.entities.OutputEnvelope

/**
  * @author Pavel Tomskikh
  */
class StubRestData extends OutputEnvelope {

  var stringValue: String = ""
  var seqValue: Seq[Int] = Seq()

  override def getFieldsValue: Map[String, Any] = {
    Map(
      "stringValue" -> stringValue,
      "seqValue" -> seqValue)
  }
}
