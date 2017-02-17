package com.bwsw.sj.examples.sflow.module.input

import com.bwsw.sj.common.engine.StreamingValidator
import com.bwsw.sj.common.rest.entities.module.InstanceMetadata

class Validator extends StreamingValidator {

  override def validate(instanceMetadata: InstanceMetadata): Boolean = {
    true
  }
}