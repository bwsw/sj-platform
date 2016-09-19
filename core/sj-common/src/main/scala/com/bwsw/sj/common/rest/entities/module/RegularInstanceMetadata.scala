package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.StreamLiterals._
import com.fasterxml.jackson.annotation.JsonProperty

class RegularInstanceMetadata extends InstanceMetadata {
  var inputs: Array[String] = Array()
  var outputs: Array[String] = Array()
  @JsonProperty("execution-plan") var executionPlan: ExecutionPlan = null
  @JsonProperty("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @JsonProperty("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @JsonProperty("event-wait-time") var eventWaitTime: Long = 1000

  override def asModelInstance() = {
    val modelInstance = new RegularInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.stateManagement = this.stateManagement
    modelInstance.stateFullCheckpoint = this.stateFullCheckpoint
    modelInstance.eventWaitTime = this.eventWaitTime
    modelInstance.inputs = this.inputs
    modelInstance.outputs = this.outputs
    modelInstance.startFrom = this.startFrom
    modelInstance.executionPlan = this.executionPlan

    modelInstance
  }

  override def prepareInstance(moduleType: String,
                            moduleName: String,
                            moduleVersion: String,
                            engineName: String,
                            engineVersion: String) = {
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(this.inputs.map(clearStreamFromMode)))
    this.executionPlan = new ExecutionPlan().fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))

    val inputStreams = getStreams(this.inputs.map(clearStreamFromMode))
    val outputStreams = this.outputs
    val streams = inputStreams.filter(s => s.streamType.equals(tStreamType)).map(_.name).union(outputStreams)
    fillStages(streams)
  }

  override def createStreams() = {
    val sjStreams = getStreams(this.inputs.map(clearStreamFromMode) ++ this.outputs)
    sjStreams.foreach(_.create())
  }

  override def getInputs() = this.inputs
}
