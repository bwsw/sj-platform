package com.bwsw.sj.stubs.module.output.data

import com.bwsw.sj.engine.core.entities.{JdbcEnvelope, OutputEnvelope}

/**
  * @author Diryavkin Dmitry
  */

class StubJdbcData extends OutputEnvelope {
  var value: Int = 0
  var testId: String = null

  override def getMapFields: Map[String, AnyRef] = {
    Map("value" -> value, "testId" -> testId)
  }
}