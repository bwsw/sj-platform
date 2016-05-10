package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations._

import scala.collection.immutable.HashMap

/**
 * Entity for base instance-json
 * Created:  13/04/2016
 *
 * @author Kseniya Tomskikh
 */
@Entity("instances")
class RegularInstance {
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
  @Property("state-management") var stateManagement: String = null
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  var parallelism: Int = 0
  var options: String = null
  @Property("start-from") var startFrom: String = null
  @Property("per-task-cores") var perTaskCores: Double = 0.0
  @Property("per-task-ram") var perTaskRam: Int = 0
  @Embedded("jvm-options") var jvmOptions: java.util.Map[String, String] = null
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = null
  var tags: String = null
  var idle: Long = 0
}



