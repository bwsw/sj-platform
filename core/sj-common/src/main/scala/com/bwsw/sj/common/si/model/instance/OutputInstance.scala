package com.bwsw.sj.common.si.model.instance

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, FrameworkStage, OutputInstanceDomain}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{AvroUtils, EngineLiterals}
import org.apache.avro.Schema

import scala.collection.JavaConverters._

class OutputInstance(name: String,
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
                     val checkpointMode: String,
                     val checkpointInterval: Long,
                     val input: String,
                     val output: String,
                     val startFrom: String,
                     val inputAvroSchema: Option[Schema] = None,
                     val executionPlan: ExecutionPlan = new ExecutionPlan(),
                     restAddress: Option[String] = None,
                     stage: FrameworkStage = FrameworkStage(),
                     status: String = EngineLiterals.ready,
                     frameworkId: String = System.currentTimeMillis().toString)
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
    status,
    frameworkId) {

  override def to: OutputInstanceDomain = {
    val serializer = new JsonSerializer
    val serviceRepository = ConnectionRepository.getServiceRepository

    new OutputInstanceDomain(
      name,
      moduleType,
      moduleName,
      moduleVersion,
      engine,
      serviceRepository.get(coordinationService).asInstanceOf[ZKServiceDomain],
      status,
      restAddress.getOrElse(""),
      description,
      countParallelism,
      serializer.serialize(options),
      perTaskCores,
      perTaskRam,
      jvmOptions.asJava,
      nodeAttributes.asJava,
      environmentVariables.asJava,
      stage,
      performanceReportingInterval,
      frameworkId,
      Array(input),
      Array(output),
      checkpointMode,
      checkpointInterval,
      executionPlan,
      startFrom,
      AvroUtils.schemaToJson(inputAvroSchema))
  }

  override def inputsOrEmptyList: Array[String] = Array(input)
}
