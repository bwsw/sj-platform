package com.bwsw.sj.common.DAL.model.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.service.ZKService
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.rest.DTO.module.{ExecutionPlan, InstanceData, OutputInstanceData}
import com.bwsw.sj.common.utils.SjStreamUtils._
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
  * Entity for output-streaming instance-json
  *
  * @author Kseniya Tomskikh
  */
class OutputInstance(override val name: String,
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
  @Property("start-from") var startFrom: String = "newest"

  override def asProtocolInstance(): InstanceData = {
    val protocolInstance = new OutputInstanceData()
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

  override def getInputsWithoutStreamMode() = this.inputs.map(clearStreamFromMode)
}
