package com.bwsw.sj.crud.rest.model.instance

import com.bwsw.sj.common.si.model.instance.OutputInstance
import com.bwsw.sj.common.utils.{AvroUtils, EngineLiterals, RestLiterals}

class OutputInstanceApi(name: String,
                        coordinationService: String,
                        val checkpointMode: String,
                        val checkpointInterval: Long,
                        val input: String,
                        val output: String,
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

  override def to(moduleType: String, moduleName: String, moduleVersion: String): OutputInstance = {
    new OutputInstance(
      name,
      description,
      parallelism,
      options,
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
      checkpointMode,
      checkpointInterval,
      input,
      output,
      startFrom,
      AvroUtils.mapToSchema(inputAvroSchema))
  }
}
