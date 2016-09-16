package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.{ExecutionPlan, OutputInstance}
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonProperty

class OutputInstanceMetadata extends InstanceMetadata {
  @JsonProperty("execution-plan") var executionPlan: ExecutionPlan = null
  @JsonProperty("start-from") var startFrom: String = EngineLiterals.newestStartMode
  var input: String = null
  var output: String = null

  override def asModelInstance() = {
    val modelInstance = new OutputInstance()
    super.fillModelInstance(modelInstance)

    modelInstance.inputs = Array(this.input)
    modelInstance.outputs = Array(this.output)
    modelInstance.startFrom = this.startFrom

    modelInstance
  }

  override def prepareInstance(moduleType: String,
                            moduleName: String,
                            moduleVersion: String,
                            engineName: String,
                            engineVersion: String) = {

    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(Array(clearStreamFromMode(this.input))))
    this.executionPlan = new ExecutionPlan().fillTasks(getInputs(), this.parallelism.asInstanceOf[Int], this.name)

    val streams = Array(clearStreamFromMode(input))
    fillStages(streams)
  }

  override def createStreams() = {
    val sjStreams = getStreams(Array(clearStreamFromMode(this.input)))
    sjStreams.foreach(_.create())
  }

  private def getInputs() = Array(this.input)
}
