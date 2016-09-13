package com.bwsw.sj.common.rest.entities.stream

import java.net.URI

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.GeneratorConstants._
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object GeneratorValidator {

  private val logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Validating stream generator data
   *
   * @param generatorData - input parameters for stream generator being validated
   * @param generator - stream generator to fulfill data
   * @return - List of errors
   */
  def validate(generatorData: GeneratorData, generator: Generator) = {
    logger.debug(s"Start generator validation.")
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    var serviceObj: Option[Service] = None
    Option(generatorData.generatorType) match {
      case Some(t) if !generatorTypes.contains(t) =>
        errors += s"Unknown 'generator-type' provided. Must be one of: ${generatorTypes.mkString("[", ", ", "]")}"
      case None =>
        errors += s"'Generator-type' is required"
      case _ =>
        if (generatorData.generatorType != "local") {
          //service
          Option(generatorData.service) match {
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
                serviceName = generatorData.service
              }

              serviceObj = serviceDAO.get(serviceName)
              if (serviceObj.isEmpty) {
                errors += s"Generator 'service' does not exist"
              } else {
                if (serviceObj.get.serviceType != "ZKCoord") {
                  errors += s"Provided generator service '$serviceName' is not of type ZKCoord"
                }
              }
          }
          //instacneCount
          if (generatorData.instanceCount <= 0)
            errors += s"Generator 'instance-count' must be a positive integer for a non-local generator-type"
        }
    }

    if (errors.isEmpty) {
      generator.generatorType = generatorData.generatorType
      if (serviceObj.isDefined) generator.service = serviceObj.get
      generator.instanceCount = generatorData.instanceCount
    }
    errors
  }
}
