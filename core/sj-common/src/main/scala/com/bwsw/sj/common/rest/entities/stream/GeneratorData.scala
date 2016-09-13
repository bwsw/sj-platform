package com.bwsw.sj.common.rest.entities.stream

import java.net.URI

import com.bwsw.sj.common.DAL.model.Generator
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceConstants, GeneratorConstants}
import com.bwsw.sj.common.utils.GeneratorConstants._
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable.ArrayBuffer

case class GeneratorData(@JsonProperty("generator-type") generatorType: String,
                         service: String = null,
                         @JsonProperty("instance-count") instanceCount: Int = 0) {

  def asModelGenerator() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    this.generatorType match {
      case GeneratorConstants.local => new Generator(this.generatorType)
      case _ => new Generator(this.generatorType, serviceDAO.get(this.service).get, this.instanceCount)
    }
  }

  def validate() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    Option(this.generatorType) match {
      case Some(t) =>
        //instacneCount
        if (this.instanceCount <= 0)
          errors += s"Generator 'instance-count' must be a positive integer for a non-local generator type"

        if (!generatorTypes.contains(t)) {
          errors += s"Unknown 'generator-type' provided. Must be one of: ${generatorTypes.mkString("[", ", ", "]")}"
        } else {
          if (this.generatorType != GeneratorConstants.local) {
            //service
            Option(this.service) match {
              case None =>
                errors += s"Generator 'service' is required for a non-local generator type"
              case Some(s) =>
                var serviceName: String = ""
                if (s contains "://") {
                  val generatorUrl = new URI(s)
                  if (!generatorUrl.getScheme.equals("service-zk")) {
                    errors += s"Generator 'service' uri protocol prefix must be 'service-zk://'. Or use a plain service name instead"
                  } else {
                    serviceName = generatorUrl.getAuthority
                  }
                } else {
                  serviceName = this.service
                }

                val serviceObj = serviceDAO.get(serviceName)
                if (serviceObj.isEmpty) {
                  errors += s"Generator 'service' does not exist"
                } else {
                  if (serviceObj.get.serviceType != ServiceConstants.zookeeperServiceType) {
                    errors += s"Provided generator service '$serviceName' is not of type ${ServiceConstants.zookeeperServiceType}"
                  }
                }
            }
          }
        }
      case None =>
        errors += s"'Generator-type' is required"
      case _ =>
    }

    errors
  }
}