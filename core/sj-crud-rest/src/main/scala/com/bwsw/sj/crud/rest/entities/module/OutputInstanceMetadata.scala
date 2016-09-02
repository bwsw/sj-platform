package com.bwsw.sj.crud.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty

class OutputInstanceMetadata extends InstanceMetadata {
  @JsonProperty("start-from") var startFrom: String = null
  var input: String = null
  var output: String = null
}
