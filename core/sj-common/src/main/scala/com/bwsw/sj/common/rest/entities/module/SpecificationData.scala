package com.bwsw.sj.common.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty

case class SpecificationData(var name: String,
                               var description: String,
                               var version: String,
                               var author: String,
                               var license: String,
                               inputs: Map[String, Any],
                               outputs: Map[String, Any],
                               @JsonProperty("module-type") var moduleType: String,
                               @JsonProperty("engine-name") var engineName: String,
                               @JsonProperty("engine-version") var engineVersion: String,
                               options: Map[String, Any],
                               @JsonProperty("validator-class") var validateClass: String,
                               @JsonProperty("executor-class") executorClass: String)
