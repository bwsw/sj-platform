package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty

class Specification(var name: String,
                    var description: String,
                    var version: String,
                    var author: String,
                    var license: String,
                    var inputs: IOstream,
                    var outputs: IOstream,
                    @JsonProperty("module-type") var moduleType: String,
                    var engine: String,
                    var options: Map[String, Any],
                    @JsonProperty("validator-class") val validateClass: String,
                    @JsonProperty("executor-class") val executorClass: String)
