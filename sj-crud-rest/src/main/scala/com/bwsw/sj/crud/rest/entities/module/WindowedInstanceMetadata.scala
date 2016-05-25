package com.bwsw.sj.crud.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Protocol entity for instance of windowed module
  * Created: 29/04/2016
  *
  * @author Kseniya Tomskikh
  */
class WindowedInstanceMetadata extends InstanceMetadata {
  @JsonProperty("state-management") var stateManagement: String = null
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  @JsonProperty("event-wait-time") var eventWaitTime: Long = 0
  @JsonProperty("time-windowed") var timeWindowed: Int = 0
  @JsonProperty("window-full-max") var windowFullMax: Int = 0
}
