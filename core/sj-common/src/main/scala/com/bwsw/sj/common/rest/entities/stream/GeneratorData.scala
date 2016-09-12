package com.bwsw.sj.common.rest.entities.stream

import com.fasterxml.jackson.annotation.JsonProperty

case class GeneratorData(@JsonProperty("generator-type") generatorType: String,
                         service: String = null,
                         @JsonProperty("instance-count") instanceCount: Int = 0)