package com.bwsw.sj.common.dal.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.rest.model.module.{BatchInstanceApi, ExecutionPlan, InstanceApi}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
  * Entity for batch instance-json
  *
  * @author Kseniya Tomskikh
  */
class BatchInstanceDomain(override val name: String,
                          override val moduleType: String,
                          override val moduleName: String,
                          override val moduleVersion: String,
                          override val engine: String,
                          override val coordinationService: ZKServiceDomain)
  extends InstanceDomain(name, moduleType, moduleName, moduleVersion, engine, coordinationService) with AvroSchemaForInstance {

  var inputs: Array[String] = Array()
  var window: Int = 1
  @Property("sliding-interval") var slidingInterval: Int = 1
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @Property("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @Property("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @Property("event-wait-idle-time") var eventWaitIdleTime: Long = 1000

  override def asProtocolInstance(): InstanceApi = {
    val protocolInstance = new BatchInstanceApi()
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

    protocolInstance.inputAvroSchema = this.inputAvroSchema.map { s =>
      val serializer = new JsonSerializer()
      serializer.deserialize[Map[String, Any]](s)
    }

    protocolInstance
  }

  override def getInputsWithoutStreamMode(): Array[String] = this.inputs.map(clearStreamFromMode)
}
