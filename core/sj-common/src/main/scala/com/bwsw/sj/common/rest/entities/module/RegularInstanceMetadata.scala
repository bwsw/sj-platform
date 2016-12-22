package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.StreamLiterals._
import com.fasterxml.jackson.annotation.JsonProperty
import com.bwsw.sj.common.utils.SjStreamUtils._

class RegularInstanceMetadata extends InstanceMetadata {
  var inputs: Array[String] = Array()
  var outputs: Array[String] = Array()
  @JsonProperty("checkpoint-mode") var checkpointMode: String = null
  @JsonProperty("checkpoint-interval") var checkpointInterval: Long = Long.MinValue
  @JsonProperty("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @JsonProperty("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @JsonProperty("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @JsonProperty("event-wait-time") var eventWaitTime: Long = 1000

  override def asModelInstance() = {
    val modelInstance = new RegularInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.checkpointMode = this.checkpointMode
    modelInstance.checkpointInterval = this.checkpointInterval
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
    val clearInputs = this.inputs.map(clearStreamFromMode)
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(clearInputs))
    this.executionPlan.fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))

    val inputStreams = getStreams(clearInputs)
    val outputStreams = this.outputs
    val streams = inputStreams.filter(s => s.streamType.equals(tstreamType)).map(_.name).union(outputStreams)
    fillStages(streams)
  }

  override def createStreams() = {
    val sjStreams = getStreams(this.inputs.map(clearStreamFromMode) ++ this.outputs)
    sjStreams.foreach(_.create())
  }

  override def inputsOrEmptyList() = this.inputs
}
