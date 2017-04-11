package com.bwsw.sj.stubs.module.output

import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}

import scala.collection.mutable.ArrayBuffer

/**
  * @author Kseniya Tomskikh
  */
class StubOutputValidator extends StreamingValidator {
  override def validate(options: Map[String, Any]): ValidationInfo = {
    ValidationInfo(options.nonEmpty, ArrayBuffer("Options have to be non-empty."))
  }

}
