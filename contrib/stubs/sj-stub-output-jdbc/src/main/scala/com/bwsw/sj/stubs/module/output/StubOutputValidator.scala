package com.bwsw.sj.stubs.module.output

import com.bwsw.sj.common.engine.StreamingValidator

/**
 * @author Kseniya Tomskikh
 */
class StubOutputValidator extends StreamingValidator {
  override def validate(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }

}
