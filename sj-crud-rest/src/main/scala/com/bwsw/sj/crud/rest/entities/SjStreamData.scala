package com.bwsw.sj.crud.rest.entities

import com.bwsw.common.DAL.Entity

/**
  * Entity for stream
  */
case class SjStreamData(var name: String,
                        description: String,
                        partitions: Int,
                        service: String,
                        tags: String,
                        generator: Array[String]) extends Entity