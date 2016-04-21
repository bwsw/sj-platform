package com.bwsw.sj.common.entities

import com.fasterxml.jackson.annotation.JsonProperty

class Specification(val name: String,
                    val description: String,
                    val version: String,
                    val author: String,
                    val license: String,
                    val inputs: IOstream,
                    val outputs: IOstream,
                    @JsonProperty("module-type") val moduleType: String,
                    val engine: String,
                    val options: Map[String, Any],
                    @JsonProperty("validator-class") val validateClass: String,
                    @JsonProperty("executor-class") val executorClass: String)
