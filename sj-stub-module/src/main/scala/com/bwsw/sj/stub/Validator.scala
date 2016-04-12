package com.bwsw.sj.stub

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.module.StreamingValidator

class Validator extends StreamingValidator {

  override def validateOptions(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }

  override val serializer: Serializer = new JsonSerializer()
}