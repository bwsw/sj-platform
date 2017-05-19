package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.model.instance.RegularInstance
import com.bwsw.sj.common.utils.{AvroUtils, EngineLiterals, RestLiterals}

class RegularInstanceApi(name: String,
                         coordinationService: String,
                         val checkpointMode: String,
                         val checkpointInterval: Long,
                         val inputs: Array[String],
                         val outputs: Array[String],
                         description: String = RestLiterals.defaultDescription,
                         parallelism: Any = 1,
                         options: Map[String, Any] = Map(),
                         perTaskCores: Double = 1,
                         perTaskRam: Int = 1024,
                         jvmOptions: Map[String, String] = Map(),
                         nodeAttributes: Map[String, String] = Map(),
                         environmentVariables: Map[String, String] = Map(),
                         performanceReportingInterval: Long = 60000,
                         val startFrom: String = EngineLiterals.newestStartMode,
                         val stateManagement: String = EngineLiterals.noneStateMode,
                         val stateFullCheckpoint: Int = 100,
                         val eventWaitIdleTime: Long = 1000,
                         val inputAvroSchema: Map[String, Any] = Map())
  extends InstanceApi(
    name,
    coordinationService,
    description,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    environmentVariables,
    performanceReportingInterval) {

  override def to(moduleType: String, moduleName: String, moduleVersion: String): RegularInstance = {
    val serializer = new JsonSerializer()

    new RegularInstance(
      name,
      description,
      parallelism,
      serializer.serialize(options),
      perTaskCores,
      perTaskRam,
      jvmOptions,
      nodeAttributes,
      coordinationService,
      environmentVariables,
      performanceReportingInterval,
      moduleName,
      moduleVersion,
      moduleType,
      getEngine(moduleType, moduleName, moduleVersion),
      inputs,
      outputs,
      checkpointMode,
      checkpointInterval,
      startFrom,
      stateManagement,
      stateFullCheckpoint,
      eventWaitIdleTime,
      AvroUtils.mapToSchema(inputAvroSchema))
  }
}
