package com.bwsw.sj.common.dal.model.instance

import java.util

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import com.bwsw.sj.common.utils.StreamUtils._
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
  * Domain entity for [[EngineLiterals.batchStreamingType]] instance
  *
  * @author Kseniya Tomskikh
  */
class BatchInstanceDomain(override val name: String,
                          override val moduleType: String,
                          override val moduleName: String,
                          override val moduleVersion: String,
                          override val engine: String,
                          override val coordinationService: ZKServiceDomain,
                          override val status: String = EngineLiterals.ready,
                          override val restAddress: String = "",
                          override val description: String = RestLiterals.defaultDescription,
                          override val parallelism: Int = 1,
                          override val options: String = "{}",
                          override val perTaskCores: Double = 0.1,
                          override val perTaskRam: Int = 32,
                          override val jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String](),
                          override val nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String](),
                          override val environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String](),
                          override val stage: FrameworkStage = FrameworkStage(),
                          override val performanceReportingInterval: Long = 60000,
                          override val frameworkId: String = System.currentTimeMillis().toString,
                          var inputs: Array[String] = Array(),
                          outputs: Array[String] = Array(),
                          var window: Int = 1,
                          @Property("sliding-interval") var slidingInterval: Int = 1,
                          @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan(),
                          @Property("start-from") var startFrom: String = EngineLiterals.newestStartMode,
                          @Property("state-management") var stateManagement: String = EngineLiterals.noneStateMode,
                          @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 100,
                          @Property("event-wait-idle-time") var eventWaitIdleTime: Long = 1000,
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

  override def getInputsWithoutStreamMode: Array[String] = inputs.map(clearStreamFromMode)
}
