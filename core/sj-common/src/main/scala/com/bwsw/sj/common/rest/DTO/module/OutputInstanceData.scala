package com.bwsw.sj.common.rest.DTO.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.service.ZKService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._

class OutputInstanceData extends InstanceData with AvroSchemaForInstanceMetadata{
  var checkpointMode: String = null
  var checkpointInterval: Long = Long.MinValue
  var executionPlan: ExecutionPlan = new ExecutionPlan()
  var startFrom: String = EngineLiterals.newestStartMode
  var input: String = null
  var output: String = null

  override def asModelInstance() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val service = serviceDAO.get(this.coordinationService).get.asInstanceOf[ZKService]

    val modelInstance = new OutputInstance(name, moduleType, moduleName, moduleVersion, engine, service, checkpointMode)
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
                               engineVersion: String) = {
    val clearInputs = Array(clearStreamFromMode(this.input))
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(clearInputs))
    this.executionPlan.fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))
  }

  override def createStreams() = {
    val sjStreams = getStreams(Array(clearStreamFromMode(this.input)))
    sjStreams.foreach(_.create())
  }

  override def inputsOrEmptyList() = Array(this.input)
}
