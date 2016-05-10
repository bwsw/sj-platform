package com.bwsw.sj.crud.rest.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created: 29/04/2016
  *
  * @author Kseniya Tomskikh
  */
case class ModuleSpecification(var name: String,
                               var description: String,
                               var version: String,
                               var author: String,
                               var license: String,
                               inputs: Map[String, Any],
                               outputs: Map[String, Any],
                               @JsonProperty("module-type") var moduleType: String,
                               var engine: String,
                               options: Map[String, Any],
                               @JsonProperty("validator-class") var validateClass: String,
                               @JsonProperty("executor-class") executorClass: String)
