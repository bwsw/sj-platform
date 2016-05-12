package com.bwsw.sj.crud.rest.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 29/04/2016
  *
  * @author Kseniya Tomskikh
  */
class InstanceMetadata {
  var status: String = null
  var name: String = null
  var description: String = null
  var inputs: Array[String] = null
  var outputs: Array[String] = null
  @JsonProperty("checkpoint-mode") var checkpointMode: String = null
  @JsonProperty("checkpoint-interval") var checkpointInterval: Long = 0
  @JsonProperty("state-management") var stateManagement: String = null
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  var parallelism: Any = null
  var options: Map[String, Any] = null
  @JsonProperty("start-from") var startFrom: String = null
  @JsonProperty("per-task-cores") var perTaskCores: Double = 0.0
  @JsonProperty("per-task-ram") var perTaskRam: Int = 0
  @JsonProperty("jvm-options") var jvmOptions: Map[String, String] = null
  var attributes: Map[String, String] = null
  var idle: Long = 0
  @JsonProperty("execution-plan") var executionPlan: Map[String, Any] = null
  var environments: Map[String, String] = null
}

