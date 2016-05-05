package com.bwsw.sj.crud.rest.entities



/**
  * Stream data case class
  */
case class SjStreamData(name: String,
                        description: String,
                        partitions: Int,
                        service: String,
                        streamType: String,
                        tags: String,
                        generator: GeneratorData)