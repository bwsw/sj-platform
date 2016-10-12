package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.StreamLiterals._
import com.fasterxml.jackson.annotation.JsonProperty

class WindowedInstanceMetadata extends InstanceMetadata {
  @JsonProperty("main-stream") var mainStream: String = null
  @JsonProperty("related-streams") var relatedStreams: Array[String] = Array()
  @JsonProperty("batch-fill-type") var batchFillType: BatchFillType = null
  var window: Int = 1
  @JsonProperty("sliding-interval") var slidingInterval: Int = 1
  var outputs: Array[String] = Array()
  @JsonProperty("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @JsonProperty("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @JsonProperty("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @JsonProperty("event-wait-time") var eventWaitTime: Long = 1000

  override def asModelInstance() = {
    val modelInstance = new WindowedInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.mainStream = this.mainStream
    modelInstance.relatedStreams = this.relatedStreams
    modelInstance.batchFillType = this.batchFillType
    modelInstance.window = this.window
    modelInstance.slidingInterval = this.slidingInterval
    modelInstance.eventWaitTime = this.eventWaitTime
    modelInstance.stateManagement = this.stateManagement
    modelInstance.stateFullCheckpoint = this.stateFullCheckpoint
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
    val clearInputs = getInputs().map(clearStreamFromMode)
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(clearInputs))
    this.executionPlan.fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))

    val inputStreams = getStreams(clearInputs)
    val outputStreams = this.outputs
    val streams = inputStreams.filter(s => s.streamType.equals(tStreamType)).map(_.name).union(outputStreams)
    fillStages(streams)
  }

  override def createStreams() = {
    val inputs = getInputs()
    val sjStreams = getStreams(inputs.map(clearStreamFromMode) ++ this.outputs)
    sjStreams.foreach(_.create())
  }

  override def getInputs() = this.relatedStreams :+ this.mainStream
}
