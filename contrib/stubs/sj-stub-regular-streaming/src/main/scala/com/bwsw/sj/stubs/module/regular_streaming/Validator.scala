package com.bwsw.sj.stubs.module.regular_streaming

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}

import scala.collection.mutable.ArrayBuffer

class Validator extends StreamingValidator {

  override def validate(options: String): ValidationInfo = {
    val serializer = new JsonSerializer()
    ValidationInfo(serializer.deserialize[Map[String, Any]](options).nonEmpty, ArrayBuffer("Options have to be non-empty."))
  }
}