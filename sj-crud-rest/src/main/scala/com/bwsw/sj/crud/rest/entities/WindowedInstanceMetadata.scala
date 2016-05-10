package com.bwsw.sj.crud.rest.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 29/04/2016
  *
  * @author Kseniya Tomskikh
  */
class WindowedInstanceMetadata extends InstanceMetadata {
  @JsonProperty("time-windowed") var timeWindowed: Int = 0
  @JsonProperty("window-full-max") var windowFullMax: Int = 0
}
