package com.bwsw.sj.crud.rest

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Stream data case class
  */
case class SjStreamData(name: String,
                        description: String,
                        partitions: Int,
                        service: String,
                        @JsonProperty("stream-type") streamType: String,
                        tags: String,
                        generator: GeneratorData)