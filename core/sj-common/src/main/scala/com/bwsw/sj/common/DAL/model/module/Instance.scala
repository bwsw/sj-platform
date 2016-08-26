package com.bwsw.sj.common.DAL.model.module

import com.bwsw.sj.common.DAL.model.ZKService
import org.mongodb.morphia.annotations.{Embedded, Entity, Id, Property}

/**
  * Entity for base instance-json
  *
  *
  * @author Kseniya Tomskikh
  */
@Entity("instances")
class Instance {
  @Property("module-type") var moduleType: String = null
  @Property("module-name") var moduleName: String = null
  @Property("module-version") var moduleVersion: String = null
  var status: String = null
  @Id var name: String = null
  var description: String = null
  var inputs: Array[String] = null
  var outputs: Array[String] = null
  @Property("checkpoint-mode") var checkpointMode: String = null
  @Property("checkpoint-interval") var checkpointInterval: Long = 0
  var parallelism: Int = 0
  var options: String = null
  @Property("per-task-cores") var perTaskCores: Double = 0.0
  @Property("per-task-ram") var perTaskRam: Int = 0
  @Embedded("jvm-options") var jvmOptions: java.util.Map[String, String] = null
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = null
  @Property("node-attributes") var nodeAttributes: java.util.Map[String, String] = null
  @Embedded("coordination-service") var coordinationService: ZKService = null
  @Property("environment-variables") var environmentVariables: java.util.Map[String, String] = null
  var stages: java.util.Map[String, InstanceStage] = null
  @Property("performance-reporting-interval") var performanceReportingInterval: Long = 0
  var engine: String = null
}
