package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.EngineLiterals

/**
  * Provides a wrapper for message that is formed by [[EngineLiterals.outputStreamingType]] engine.
  */

abstract class OutputEnvelope {
  def getFieldsValue: Map[String, Any]
}
