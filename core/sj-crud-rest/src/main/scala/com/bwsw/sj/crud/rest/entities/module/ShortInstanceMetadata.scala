package com.bwsw.sj.crud.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty

case class ShortInstanceMetadata(var name: String,
                                 @JsonProperty("module-type") var moduleType: String,
                                 @JsonProperty("module-name") var moduleName: String,
                                 @JsonProperty("module-version") var moduleVersion: String,
                                 var description: String,
                                 var status: String)
