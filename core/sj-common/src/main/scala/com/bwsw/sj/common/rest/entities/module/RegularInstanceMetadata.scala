package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.StreamLiterals._
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.JavaConversions._

class RegularInstanceMetadata extends InstanceMetadata {
  var inputs: Array[String] = Array()
  var outputs: Array[String] = Array()
  @JsonProperty("execution-plan") var executionPlan: Map[String, Any] = null
  //todo используется только для того, чтобы показывать пользователям, мб стоит его заполнять,
  // а потом только конвертировать в модельный инстанс
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

    modelInstance
  }

  override def fillInstance(moduleType: String,
                            moduleName: String,
                            moduleVersion: String,
                            engineName: String,
                            engineVersion: String) = {

    val instance = super.fillInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion).asInstanceOf[RegularInstance]
    castParallelismToNumber(this.inputs.map(clearStreamFromMode).toSet)

    val executionPlan = createExecutionPlan()
    instance.executionPlan = executionPlan

    val inputStreams = getStreams(this.inputs.map(clearStreamFromMode))
    val outputStreams = this.outputs
    val streams = inputStreams.filter(s => s.streamType.equals(tStreamType)).map(_.name).toArray.union(outputStreams)
    val stages = createStages(streams)
    instance.stages = mapAsJavaMap(stages)

    instance
  }

  override def createStreams() = {
    val sjStreams = getStreams(this.inputs.map(clearStreamFromMode) ++ this.outputs)
    sjStreams.foreach(_.create())
  }

  override def getInputs() = this.inputs.map(clearStreamFromMode)
}
