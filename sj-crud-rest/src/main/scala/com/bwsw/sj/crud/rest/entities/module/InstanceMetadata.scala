package com.bwsw.sj.crud.rest.entities.module

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
  @JsonProperty(required = false) var inputs: Array[String] = null
  @JsonProperty(required = false) var outputs: Array[String] = null
  @JsonProperty("checkpoint-mode") var checkpointMode: String = null
  @JsonProperty("checkpoint-interval") var checkpointInterval: Long = 0
  var parallelism: Any = null
  var options: Map[String, Any] = null
  @JsonProperty("start-from") var startFrom: String = null
  @JsonProperty("per-task-cores") var perTaskCores: Double = 0.0
  @JsonProperty("per-task-ram") var perTaskRam: Int = 0
  @JsonProperty("jvm-options") var jvmOptions: Map[String, String] = null
  @JsonProperty("node-attributes") var nodeAttributes: Map[String, String] = null
  @JsonProperty("execution-plan") var executionPlan: Map[String, Any] = null
  @JsonProperty("coordination-service") var coordinationService: String = null
  @JsonProperty("environment-variables") var environmentVariables: Map[String, String] = null
}

