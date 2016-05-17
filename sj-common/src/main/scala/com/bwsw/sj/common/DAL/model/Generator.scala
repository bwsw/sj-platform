package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.{Property, Reference}

class Generator() {
  @Property("generator-type") var generatorType: String = null
  @Reference var service: Service = null
  @Property("instance-count") var instanceCount: Int = 0

  def this(generatorType: String) = {
    this()
    this.generatorType = generatorType
  }

  def this(generatorType: String, service: Service, instanceCount: Int) = {
    this(generatorType)
    this.service = service
    this.instanceCount = instanceCount
  }
}
