package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Entity for simple instance-json
  * Created:  13/04/2016
  * @author Kseniya Tomskikh
  */
case class RegularInstanceMetadata(var name: String,
                                   var description: String,
                                   var inputs: List[String],
                                   var outputs: List[String],
                                   @JsonProperty("checkpoint-mode") var checkpointMode: String,
                                   @JsonProperty("checkpoint-interval") var checkpointInterval: Long,
                                   @JsonProperty("state-management") var stateManagement: String,
                                   @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int,
                                   var parallelism: Int,
                                   var options: Map[String, Any],
                                   @JsonProperty("start-from") var startFrom: String,
                                   @JsonProperty("per-task-cores") var perTaskCores: Int,
                                   @JsonProperty("per-task-ram") var perTaskRam: Int,
                                   @JsonProperty("jvm-options") var jvmOptions: Map[String, Any],
                                   var uuid: String,
                                   @JsonProperty("module-type") var moduleType: String,
                                   @JsonProperty("module-name") var moduleName: String,
                                   @JsonProperty("module-version") var moduleVersion: String,
                                   var status: String,
                                   @JsonProperty("execution-plan") var executionPlan: ExecutionPlan,
                                   var tags: String,
                                    var idle: Long) extends InstanceMetadata
