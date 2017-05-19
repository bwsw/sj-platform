package com.bwsw.sj.common.dal.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.rest.model.module.{InstanceApi, RegularInstanceApi}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.StreamUtils._
import org.mongodb.morphia.annotations._

/**
  * Domain entity for [[EngineLiterals.regularStreamingType]] instance
  *
  * @author Kseniya Tomskikh
  */
class RegularInstanceDomain(override val name: String,
                            override val moduleType: String,
                            override val moduleName: String,
                            override val moduleVersion: String,
                            override val engine: String,
                            override val coordinationService: ZKServiceDomain,
                            @PropertyField("checkpoint-mode") val checkpointMode: String)
  extends InstanceDomain(name, moduleType, moduleName, moduleVersion, engine, coordinationService) with InputAvroSchema {

  var inputs: Array[String] = Array()
  @Property("checkpoint-interval") var checkpointInterval: Long = 0
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @Property("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @Property("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @Property("event-wait-idle-time") var eventWaitIdleTime: Long = 1000

  override def asProtocolInstance(): InstanceApi = {
    val protocolInstance = new RegularInstanceApi()
    super.fillProtocolInstance(protocolInstance)
    protocolInstance.checkpointMode = this.checkpointMode
    protocolInstance.checkpointInterval = this.checkpointInterval
    protocolInstance.executionPlan = this.executionPlan
    protocolInstance.startFrom = this.startFrom
    protocolInstance.stateManagement = this.stateManagement
    protocolInstance.stateFullCheckpoint = this.stateFullCheckpoint
    protocolInstance.eventWaitIdleTime = this.eventWaitIdleTime
    protocolInstance.inputs = this.inputs
    protocolInstance.outputs = this.outputs

    val serializer = new JsonSerializer()
    protocolInstance.inputAvroSchema = serializer.deserialize[Map[String, Any]](this.inputAvroSchema)

    protocolInstance
  }

  override def getInputsWithoutStreamMode(): Array[String] = this.inputs.map(clearStreamFromMode)
}





