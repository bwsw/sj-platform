package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Entity for simple instance-json
  * Created:  13/04/2016
  * @author Kseniya Tomskikh
  */
case class SimpleInstanceMetadata(var name: String,
                                  var uuid: String,
                                  var description: String,
                                  var inputs: List[String],
                                  var outputs: List[String],
                                  @JsonProperty("checkpoint-mode") var checkpointMode: String,
                                  @JsonProperty("checkpoint-interval") var checkpointInterval: Int,
                                  @JsonProperty("state-management") var stateManagement: String,
                                  @JsonProperty("checkpoint-full-interval") var checkpointFullInterval: Int,
                                  var parallelism: Int,
                                  var options: Map[String, Any],
                                  @JsonProperty("start-from") var startFrom: Any) extends InstanceMetadata
