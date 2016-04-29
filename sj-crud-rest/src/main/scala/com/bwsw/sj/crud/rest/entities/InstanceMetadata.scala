package com.bwsw.sj.crud.rest.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 29/04/2016
  *
  * @author Kseniya Tomskikh
  */
case class InstanceMetadata(var status: String,
                            var name: String,
                            var description: String,
                            var inputs: Array[String],
                            var outputs: Array[String],
                            @JsonProperty("checkpoint-mode") var checkpointMode: String,
                            @JsonProperty("checkpoint-interval") var checkpointInterval: Long,
                            @JsonProperty("state-management") var stateManagement: String,
                            @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int,
                            var parallelism: Any,
                            var options: Map[String, Any],
                            @JsonProperty("start-from") var startFrom: String,
                            @JsonProperty("per-task-cores") var perTaskCores: Int,
                            @JsonProperty("per-task-ram") var perTaskRam: Int,
                            @JsonProperty("jvm-options") var jvmOptions: Map[String, String],
                            var tags: String,
                            var idle: Long,
                            @JsonProperty("time-windowed") var timeWindowed: Int,
                            @JsonProperty("window-full-max") var windowFullMax: Int)

