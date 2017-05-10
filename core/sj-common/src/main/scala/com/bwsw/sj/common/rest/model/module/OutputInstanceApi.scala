package com.bwsw.sj.common.rest.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.module.OutputInstanceDomain
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._

class OutputInstanceApi extends InstanceApi with AvroSchemaForInstanceMetadata{
  var checkpointMode: String = null
  var checkpointInterval: Long = Long.MinValue
  var executionPlan: ExecutionPlan = new ExecutionPlan()
  var startFrom: String = EngineLiterals.newestStartMode
  var input: String = null
  var output: String = null

  override def asModelInstance(): OutputInstanceDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository
    val service = serviceRepository.get(this.coordinationService).get.asInstanceOf[ZKServiceDomain]

    val modelInstance = new OutputInstanceDomain(name, moduleType, moduleName, moduleVersion, engine, service, checkpointMode)
    super.fillModelInstance(modelInstance)
    modelInstance.checkpointInterval = this.checkpointInterval
    modelInstance.inputs = Array(this.input)
    modelInstance.outputs = Array(this.output)
    modelInstance.startFrom = this.startFrom
    modelInstance.executionPlan = this.executionPlan

    val serializer = new JsonSerializer()
    modelInstance.inputAvroSchema = serializer.serialize(this.inputAvroSchema)

    modelInstance
  }

  override def prepareInstance(moduleType: String,
                               moduleName: String,
                               moduleVersion: String,
                               engineName: String,
                               engineVersion: String): Unit = {
    val clearInputs = Array(clearStreamFromMode(this.input))
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(clearInputs))
    this.executionPlan.fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))
  }

  override def createStreams(): Unit = {
    val sjStreams = getStreams(Array(clearStreamFromMode(this.input)))
    sjStreams.foreach(_.create())
  }

  override def inputsOrEmptyList(): Array[String] = Array(this.input)
}
