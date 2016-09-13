package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.Generator
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.fasterxml.jackson.annotation.JsonProperty

case class GeneratorData(@JsonProperty("generator-type") generatorType: String,
                         service: String = null,
                         @JsonProperty("instance-count") instanceCount: Int = 0) {

  def asModelGenerator() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    this.generatorType match {
      case "local" => new Generator(this.generatorType)
      case _ => new Generator(this.generatorType, serviceDAO.get(this.service).get, this.instanceCount)
    }
  }
}