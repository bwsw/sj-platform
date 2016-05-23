package com.bwsw.sj.crud.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 5/23/16
  *
  * @author Kseniya Tomskikh
  */
class RegularInstanceMetadata extends InstanceMetadata {
  @JsonProperty("state-management") var stateManagement: String = null
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 0
}
