package com.bwsw.sj.common.dal.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.service.ZKService
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.rest.model.module.{ExecutionPlan, InstanceData, RegularInstanceData}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._
import org.mongodb.morphia.annotations._

/**
  * Entity for regular instance-json
  *
  * @author Kseniya Tomskikh
  */
class RegularInstance(override val name: String,
                      override val moduleType: String,
                      override val moduleName: String,
                      override val moduleVersion: String,
                      override val engine: String,
                      override val coordinationService: ZKService,
                      @PropertyField("checkpoint-mode") val checkpointMode: String)
  extends Instance(name, moduleType, moduleName, moduleVersion, engine, coordinationService) with AvroSchemaForInstance {

  var inputs: Array[String] = Array()
  @Property("checkpoint-interval") var checkpointInterval: Long = 0
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @Property("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @Property("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @Property("event-wait-idle-time") var eventWaitIdleTime: Long = 1000

  override def asProtocolInstance(): InstanceData = {
    val protocolInstance = new RegularInstanceData()
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

    protocolInstance.inputAvroSchema = this.inputAvroSchema.map { s =>
      val serializer = new JsonSerializer()
      serializer.deserialize[Map[String, Any]](s)
    }

    protocolInstance
  }

  override def getInputsWithoutStreamMode(): Array[String] = this.inputs.map(clearStreamFromMode)
}





