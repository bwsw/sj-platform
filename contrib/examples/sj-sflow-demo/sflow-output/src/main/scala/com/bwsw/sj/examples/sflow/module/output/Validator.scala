package com.bwsw.sj.examples.sflow.module.output

import com.bwsw.sj.common.engine.StreamingValidator

/**
  * Created by diryavkin_dn on 13.01.17.
  */
class Validator extends StreamingValidator {
  override def validate(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }

}