package com.bwsw.sj.common.dal.model.instance

import java.util

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
  * Entity for output-streaming instance-json
  *
  * @author Kseniya Tomskikh
  */
class OutputInstanceDomain(override val name: String,
                           override val moduleType: String,
                           override val moduleName: String,
                           override val moduleVersion: String,
                           override val engine: String,
                           override val coordinationService: ZKServiceDomain,
                           status: String = EngineLiterals.ready,
                           restAddress: String = "",
                           description: String = RestLiterals.defaultDescription,
                           parallelism: Int = 1,
                           options: String = "{}",
                           perTaskCores: Double = 0.1,
                           perTaskRam: Int = 32,
                           jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String](),
                           nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String](),
                           environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String](),
                           stage: FrameworkStage = FrameworkStage(),
                           performanceReportingInterval: Long = 60000,
                           override val frameworkId: String = System.currentTimeMillis().toString,
                           var inputs: Array[String] = Array(),
                           outputs: Array[String] = Array(),
                           @PropertyField("checkpoint-mode") val checkpointMode: String,
                           @Property("checkpoint-interval") var checkpointInterval: Long = 0,
                           @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan(),
                           @Property("start-from") var startFrom: String = "newest",
                           @Property("input-avro-schema") var inputAvroSchema: String = "{}")
  extends InstanceDomain(
    name,
    moduleType,
    moduleName,
    moduleVersion,
    engine,
    coordinationService,
    status,
    restAddress,
    description,
    outputs,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    environmentVariables,
    stage,
    performanceReportingInterval,
    frameworkId) {

  override def getInputsWithoutStreamMode() = this.inputs.map(clearStreamFromMode)
}
