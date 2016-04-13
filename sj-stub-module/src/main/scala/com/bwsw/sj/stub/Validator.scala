package com.bwsw.sj.stub

import com.bwsw.sj.common.module.StreamingValidator

class Validator extends StreamingValidator {

  override def validate(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }
}