package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.fasterxml.jackson.annotation.JsonProperty

class OutputInstanceMetadata extends InstanceMetadata {
  @JsonProperty("execution-plan") var executionPlan: Map[String, Any] = null
  @JsonProperty("start-from") var startFrom: String = null
  var input: String = null
  var output: String = null

  override def toModelInstance() = {
    val modelInstance = new OutputInstance()
    super.fillModelInstance(modelInstance)

    modelInstance.inputs = Array(this.input)
    modelInstance.outputs = Array(this.output)
    modelInstance.startFrom = this.startFrom

    modelInstance
  }
}