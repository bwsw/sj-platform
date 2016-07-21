package com.bwsw.examples.sflow.module.output

import com.bwsw.sj.common.engine.StreamingValidator

/**
  * Created: 23/06/16
  *
  * @author Kseniya Mikhaleva
  */
class SflowOutputValidator extends StreamingValidator {
  override def validate(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }

}
