package com.bwsw.sj.common.DAL.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.rest.entities.module.{BatchInstanceMetadata, ExecutionPlan, InstanceMetadata}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
  * Entity for batch instance-json
  *
  * @author Kseniya Tomskikh
  */
class BatchInstance() extends Instance with AvroSchemaForInstance {
  var inputs: Array[String] = Array()
  var window: Int = 1
  @Property("sliding-interval") var slidingInterval: Int = 1
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @Property("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @Property("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @Property("event-wait-idle-time") var eventWaitIdleTime: Long = 1000

  override def asProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new BatchInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.inputs = this.inputs
    protocolInstance.window = this.window
    protocolInstance.slidingInterval = this.slidingInterval
    protocolInstance.eventWaitIdleTime = this.eventWaitIdleTime
    protocolInstance.executionPlan = this.executionPlan
    protocolInstance.stateManagement = this.stateManagement
    protocolInstance.stateFullCheckpoint = this.stateFullCheckpoint
    protocolInstance.outputs = this.outputs
    protocolInstance.startFrom = this.startFrom

    val serializer = new JsonSerializer()
    protocolInstance.inputAvroSchema = serializer.deserialize[Map[String, Any]](this.inputAvroSchema)

    protocolInstance
  }

  override def getInputsWithoutStreamMode(): Array[String] = this.inputs.map(clearStreamFromMode)
}
