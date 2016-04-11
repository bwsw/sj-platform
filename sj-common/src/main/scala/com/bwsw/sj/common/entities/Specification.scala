package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty

class Specification(var name: String,
                    var description: String,
                    var version: String,
                    var author: String,
                    var license: String,
                    inputs: IOstream,
                    outputs: IOstream,
                    @JsonProperty("module-type") var moduleType: String,
                    var engine: String,
                    options: Map[String, Any],
                    @JsonProperty("validator-class") var validateClass: String,
                    @JsonProperty("executor-class") executorClass: String)
