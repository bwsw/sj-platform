package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.JavaConversions._

class OutputInstanceMetadata extends InstanceMetadata {
  @JsonProperty("execution-plan") var executionPlan: Map[String, Any] = null
  //todo используется только для того, чтобы показывать пользователям, мб стоит его заполнять,
  // а потом только конвертировать в модельный инстанс
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

  override def fillInstance(moduleType: String,
                            moduleName: String,
                            moduleVersion: String,
                            engineName: String,
                            engineVersion: String) = {

    val instance = super.fillInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion).asInstanceOf[OutputInstance]
    castParallelismToNumber(Set(clearStreamFromMode(this.input)))

    val executionPlan = createExecutionPlan()
    instance.executionPlan = executionPlan

    val streams = Array(clearStreamFromMode(input))
    val stages = createStages(streams)
    instance.stages = mapAsJavaMap(stages)

    instance
  }

  override def createStreams() = {
    val sjStreams = getStreams(Array(clearStreamFromMode(this.input)))
    sjStreams.foreach(_.create())
  }

  override def getInputs() = Array(clearStreamFromMode(this.input))
}
