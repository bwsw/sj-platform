package com.bwsw.sj.examples.sflow.module.output

import com.bwsw.sj.common.engine.StreamingValidator

/**
  * @author Kseniya Mikhaleva
  */
class SflowOutputValidator extends StreamingValidator {
  override def validate(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }

}
