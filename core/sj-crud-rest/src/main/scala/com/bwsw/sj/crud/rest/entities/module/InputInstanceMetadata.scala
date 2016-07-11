package com.bwsw.sj.crud.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 11/07/2016
  *
  * @author Kseniya Tomskikh
  */
class InputInstanceMetadata extends InstanceMetadata {
  @JsonProperty("lookup-history") var lookupHistory: Long = 0
  @JsonProperty("queue-max-size") var queueMaxSize: Long = 0
  @JsonProperty("eviction-policy") var evictionPolicy: String = null
}
