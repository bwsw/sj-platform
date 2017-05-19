package com.bwsw.sj.common.dal.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.rest.model.module.{InstanceApi, OutputInstanceApi}
import com.bwsw.sj.common.utils.StreamUtils._
import org.mongodb.morphia.annotations.{Embedded, Property}
import com.bwsw.sj.common.utils.EngineLiterals

/**
  * Domain entity for [[EngineLiterals.outputStreamingType]] instance
  *
  * @author Kseniya Tomskikh
  */
class OutputInstanceDomain(override val name: String,
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
  @Property("start-from") var startFrom: String = "newest"

  override def asProtocolInstance(): InstanceApi = {
    val protocolInstance = new OutputInstanceApi()
    super.fillProtocolInstance(protocolInstance)
    protocolInstance.checkpointMode = this.checkpointMode
    protocolInstance.checkpointInterval = this.checkpointInterval
    protocolInstance.executionPlan = this.executionPlan
    protocolInstance.input = this.inputs.head
    protocolInstance.output = this.outputs.head
    protocolInstance.startFrom = this.startFrom

    val serializer = new JsonSerializer()
    protocolInstance.inputAvroSchema = serializer.deserialize[Map[String, Any]](this.inputAvroSchema)

    protocolInstance
  }

  override def getInputsWithoutStreamMode(): Array[String] = this.inputs.map(clearStreamFromMode)
}
