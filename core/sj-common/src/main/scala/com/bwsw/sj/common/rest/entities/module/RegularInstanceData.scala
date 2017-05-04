package com.bwsw.sj.common.rest.entities.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.model.service.ZKService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._

class RegularInstanceData extends InstanceData with AvroSchemaForInstanceMetadata {
  var inputs: Array[String] = Array()
  var outputs: Array[String] = Array()
  var checkpointMode: String = null
  var checkpointInterval: Long = Long.MinValue
  var executionPlan: ExecutionPlan = new ExecutionPlan()
  var startFrom: String = EngineLiterals.newestStartMode
  var stateManagement: String = EngineLiterals.noneStateMode
  var stateFullCheckpoint: Int = 100
  var eventWaitIdleTime: Long = 1000

  override def asModelInstance() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val service = serviceDAO.get(this.coordinationService).get.asInstanceOf[ZKService]

    val modelInstance = new RegularInstance(name, moduleType, moduleName, moduleVersion, engine, service, checkpointMode)
    super.fillModelInstance(modelInstance)
    modelInstance.checkpointInterval = this.checkpointInterval
    modelInstance.stateManagement = this.stateManagement
    modelInstance.stateFullCheckpoint = this.stateFullCheckpoint
    modelInstance.eventWaitIdleTime = this.eventWaitIdleTime
    modelInstance.inputs = this.inputs
    modelInstance.outputs = this.outputs
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
    val clearInputs = this.inputs.map(clearStreamFromMode)
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(clearInputs))
    this.executionPlan.fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))
  }

  override def createStreams() = {
    val sjStreams = getStreams(this.inputs.map(clearStreamFromMode) ++ this.outputs)
    sjStreams.foreach(_.create())
  }

  override def inputsOrEmptyList() = this.inputs
}
