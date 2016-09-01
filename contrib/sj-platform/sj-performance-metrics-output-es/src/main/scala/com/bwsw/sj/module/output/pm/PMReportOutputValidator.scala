package com.bwsw.sj.module.output.pm

import com.bwsw.sj.common.engine.StreamingValidator

/**
  * @author Kseniya Mikhaleva
  */
class PMReportOutputValidator extends StreamingValidator {
  override def validate(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }

}
