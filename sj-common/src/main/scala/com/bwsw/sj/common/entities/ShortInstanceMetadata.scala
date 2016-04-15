package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Entity for preview short data of instance
  * Created: 13/04/2016
  * @author Kseniya Tomskikh
  */
case class ShortInstanceMetadata(var name: String,
                                 @JsonProperty("module-type") var moduleType: String,
                                 @JsonProperty("module-name") var moduleName: String,
                                 @JsonProperty("module-version") var moduleVersion: String,
                                 var description: String,
                                 var status: String)
