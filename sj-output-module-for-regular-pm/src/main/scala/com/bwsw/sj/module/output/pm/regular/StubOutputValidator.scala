package com.bwsw.sj.module.output.pm.regular

import com.bwsw.sj.common.module.StreamingValidator

/**
  * Created: 27/05/16
  *
  * @author Kseniya Tomskikh
  */
class StubOutputValidator extends StreamingValidator {
  override def validate(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }

}
