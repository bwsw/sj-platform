package com.bwsw.sj.stubs.module.regular_streaming

import com.bwsw.sj.common.engine.StreamingValidator

class Validator extends StreamingValidator {

  override def validate(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }
}