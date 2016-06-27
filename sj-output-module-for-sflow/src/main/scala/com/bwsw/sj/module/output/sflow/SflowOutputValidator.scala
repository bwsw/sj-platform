package com.bwsw.sj.module.output.sflow

import com.bwsw.sj.common.module.StreamingValidator

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
