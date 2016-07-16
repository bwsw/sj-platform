package com.bwsw.sj.crud.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.InputTask
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 11/07/2016
  *
  * @author Kseniya Tomskikh
  */
class InputInstanceMetadata extends InstanceMetadata {
  @JsonProperty("lookup-history") var lookupHistory: Int = 0
  @JsonProperty("queue-max-size") var queueMaxSize: Int = 0
  @JsonProperty("default-eviction-policy") var defaultEvictionPolicy: String = null
  @JsonProperty("eviction-policy") var evictionPolicy: String = null
  var tasks: Map[String, InputTask] = null
}
