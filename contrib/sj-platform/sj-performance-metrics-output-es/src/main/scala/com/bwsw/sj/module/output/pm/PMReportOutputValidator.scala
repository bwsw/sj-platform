package com.bwsw.sj.module.output.pm

import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}

import scala.collection.mutable.ArrayBuffer

/**
  * @author Kseniya Mikhaleva
  */
class PMReportOutputValidator extends StreamingValidator {
  override def validate(options: Map[String, Any]): ValidationInfo = {
    ValidationInfo(options.nonEmpty, ArrayBuffer("Options have to be non-empty."))
  }

}
