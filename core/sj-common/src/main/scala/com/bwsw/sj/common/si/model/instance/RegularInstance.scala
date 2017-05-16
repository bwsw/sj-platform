package com.bwsw.sj.common.si.model.instance

import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, FrameworkStage}
import com.bwsw.sj.common.utils.EngineLiterals
import org.apache.avro.Schema

class RegularInstance(name: String,
                      description: String,
                      parallelism: Any,
                      options: Map[String, Any],
                      perTaskCores: Double,
                      perTaskRam: Int,
                      jvmOptions: Map[String, String],
                      nodeAttributes: Map[String, String],
                      coordinationService: String,
                      environmentVariables: Map[String, String],
                      performanceReportingInterval: Long,
                      moduleName: String,
                      moduleVersion: String,
                      moduleType: String,
                      engine: String,
                      val inputs: Array[String],
                      val outputs: Array[String],
                      val checkpointMode: String,
                      val checkpointInterval: Long,
                      val startFrom: String,
                      val stateManagement: String,
                      val stateFullCheckpoint: Int,
                      val eventWaitIdleTime: Long,
                      val inputAvroSchema: Option[Schema],
                      val executionPlan: ExecutionPlan = new ExecutionPlan(),
                      restAddress: Option[String] = None,
                      stage: FrameworkStage = FrameworkStage(),
                      status: String = EngineLiterals.ready)
  extends Instance(
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
    engine,
    restAddress,
    stage,
    status) {

}
