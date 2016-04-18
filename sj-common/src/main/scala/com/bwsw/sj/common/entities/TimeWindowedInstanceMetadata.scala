package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Entity for windowed instance-json
  * Created:  13/04/2016
  * @author Kseniya Tomskikh
  */
case class TimeWindowedInstanceMetadata(var uuid: String,
                                        @JsonProperty("module-type") var moduleType: String,
                                        @JsonProperty("module-name") var moduleName: String,
                                        @JsonProperty("module-version") var moduleVersion: String,
                                        var name: String,
                                        var description: String,
                                        var inputs: List[String],
                                        var outputs: List[String],
                                        @JsonProperty("checkpoint-mode") var checkpointMode: String,
                                        @JsonProperty("checkpoint-interval") var checkpointInterval: Long,
                                        @JsonProperty("state-management") var stateManagement: String,
                                        @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int,
                                        var parallelism: Int,
                                        var options: Map[String, Any],
                                        @JsonProperty("start-from") var startFrom: Any,
                                        @JsonProperty("per-task-cores") var perTaskCores: Int,
                                        @JsonProperty("per-task-ram") var perTaskRam: Int,
                                        @JsonProperty("jvm-options") var jvmOptions: Map[String, Any],
                                        @JsonProperty("time-windowed") var timeWindowed: Int,
                                        @JsonProperty("window-full-max") var windowFullMax: Int,
                                        var status: String,
                                        @JsonProperty("execution-plan") var executionPlan: ExecutionPlan) extends InstanceMetadata
