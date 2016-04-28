package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty
import org.mongodb.morphia.annotations.{Property, Embedded, Entity, Id}

import scala.collection.immutable.HashMap

/**
 * Entity for base instance-json
 * Created:  13/04/2016
  *
  * @author Kseniya Tomskikh
 */
@Entity("instances")
class RegularInstanceMetadata {
  @Property("module-type") @JsonProperty("module-type") var moduleType: String = null
  @Property("module-name") @JsonProperty("module-name") var moduleName: String = null
  @Property("module-version") @JsonProperty("module-version") var moduleVersion: String = null
  var status: String = null
  @Id var name: String = null
  var description: String = null
  var inputs: Array[String] = null
  var outputs: Array[String] = null
  @Property("checkpoint-mode") @JsonProperty("checkpoint-mode") var checkpointMode: String = null
  @Property("checkpoint-interval") @JsonProperty("checkpoint-interval") var checkpointInterval: Long = 0
  @Property("state-management") @JsonProperty("state-management") var stateManagement: String = null
  @Property("state-full-checkpoint") @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  var parallelism: Any = null
  var options: HashMap[String, Any] = null
  @Property("start-from") @JsonProperty("start-from") var startFrom: String = null
  @Property("per-task-cores") @JsonProperty("per-task-cores") var perTaskCores: Int = 0
  @Property("per-task-ram") @JsonProperty("per-task-ram") var perTaskRam: Int = 0
  @Property("jvm-options") @JsonProperty("jvm-options") var jvmOptions: HashMap[String, Any] = null
  @Property("execution-plan") @JsonProperty("execution-plan") var executionPlan: ExecutionPlan = null
  var tags: String = null
  var idle: Long = 0
}



