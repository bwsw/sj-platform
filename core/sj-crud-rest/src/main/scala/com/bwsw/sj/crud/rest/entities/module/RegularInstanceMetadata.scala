package com.bwsw.sj.crud.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty

class RegularInstanceMetadata extends InstanceMetadata {
  var inputs: Array[String] = null
  @JsonProperty("start-from") var startFrom: String = null
  @JsonProperty("state-management") var stateManagement: String = null
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  @JsonProperty("event-wait-time") var eventWaitTime: Long = 0
}
