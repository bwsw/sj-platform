package com.bwsw.sj.common.rest.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, RegularInstanceDomain}
import com.bwsw.sj.common.dal.model.module._
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._

class RegularInstanceApi extends InstanceApi with AvroSchemaForInstanceMetadata {
  var inputs: Array[String] = Array()
  var outputs: Array[String] = Array()
  var checkpointMode: String = _
  var checkpointInterval: Long = Long.MinValue
  var executionPlan: ExecutionPlan = new ExecutionPlan()
  var startFrom: String = EngineLiterals.newestStartMode
  var stateManagement: String = EngineLiterals.noneStateMode
  var stateFullCheckpoint: Int = 100
  var eventWaitIdleTime: Long = 1000

  override def asModelInstance(): RegularInstanceDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository
    val service = serviceRepository.get(this.coordinationService).get.asInstanceOf[ZKServiceDomain]

    val modelInstance = new RegularInstanceDomain(name, moduleType, moduleName, moduleVersion, engine, service, checkpointMode)
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
                               engineVersion: String): Unit = {
    val clearInputs = this.inputs.map(clearStreamFromMode)
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(clearInputs))
    this.executionPlan.fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))
  }

  override def createStreams(): Unit = {
    val sjStreams = getStreams(this.inputs.map(clearStreamFromMode) ++ this.outputs)
    sjStreams.foreach(_.create())
  }

  override def inputsOrEmptyList(): Array[String] = this.inputs
}