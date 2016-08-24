package com.bwsw.sj.crud.rest.validator.stream

import java.net.URI

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.GeneratorConstants._
import com.bwsw.sj.crud.rest.entities.stream.GeneratorData
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mendelbaum_nm on 05.05.16.
  */

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
        errors += s"Unknown 'generator-type' provided. Must be one of: $generatorTypes"
      case None =>
        errors += s"'generator-type' is required"
      case _ =>
        if (generatorData.generatorType != "local") {
          //service
          Option(generatorData.service) match {
            case Some(s) if s.isEmpty =>
              errors += s"Generator 'service' can not be empty"
            case None =>
              errors += s"Generator 'service' is required for non-'local' generator-type"
            case _ =>
              if (generatorData.service contains "://") {
                val generatorUrl = new URI(generatorData.service)
                if (!generatorUrl.getScheme.equals("service-zk")) {
                  errors += s"Generator 'service' uri protocol prefix must be 'service-zk://'. Or use plain service name instead"
                }
              }
              var serviceName: String = ""
              if (generatorData.service contains "://") {
                val generatorUrl = new URI(generatorData.service)
                if (generatorUrl.getScheme.equals("service-zk")) {
                  serviceName = generatorUrl.getAuthority
                }
              } else {
                serviceName = generatorData.service
              }
              serviceObj = serviceDAO.get(serviceName)


              if (serviceObj.isEmpty) {
                errors += s"Unknown generator 'service' provided"
              } else {
                if (serviceObj.get.serviceType != "ZKCoord") {
                  errors += s"Provided generator service '$serviceName' is not of type ZKCoord"
                }
              }
          }
          //instacneCount
          if (generatorData.instanceCount <= 0)
            errors += s"Generator 'instance-count' must be a positive integer for non-'local' generator-type"
        }
        else {
          //service
          if (Option(generatorData.service).isDefined)
            errors += s"Generator 'service' should not exist for 'local' generator-type"

          //instacneCount
          if (generatorData.instanceCount > 0)
            errors += s"Generator 'instance-count' should not exist for 'local' generator-type"
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
