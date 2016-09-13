package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonProperty

class WindowedInstanceMetadata extends InstanceMetadata {
  var inputs: Array[String] = Array()
  var outputs: Array[String] = Array()
  @JsonProperty("execution-plan") var executionPlan: Map[String, Any] = null
  @JsonProperty("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @JsonProperty("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @JsonProperty("event-wait-time") var eventWaitTime: Long = 1000
  @JsonProperty("time-windowed") var timeWindowed: Int = 0
  @JsonProperty("window-full-max") var windowFullMax: Int = 0

  override def asModelInstance() = {
    val modelInstance = new WindowedInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.timeWindowed = this.timeWindowed
    modelInstance.windowFullMax = this.windowFullMax
    modelInstance.stateManagement = this.stateManagement
    modelInstance.stateFullCheckpoint = this.stateFullCheckpoint
    modelInstance.eventWaitTime = this.eventWaitTime
    modelInstance.inputs = this.inputs
    modelInstance.outputs = this.outputs
    modelInstance.startFrom = this.startFrom

    modelInstance
  }
}
