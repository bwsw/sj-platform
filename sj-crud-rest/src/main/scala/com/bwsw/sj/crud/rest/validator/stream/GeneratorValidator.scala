package com.bwsw.sj.crud.rest.validator.stream


import java.net.URI

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mendelbaum_nm on 05.05.16.
  */

object GeneratorValidator {
  import com.bwsw.sj.common.GeneratorConstants._

  /**
    * Validating input parameters for generator
    *
    * @param params - parameters of stream generator being validated
    * @param initialData - input parameters for stream generator being validated
    * @return - List of errors
    */
  def validate(params: Generator, initialData: GeneratorData) = {

    val errors = new ArrayBuffer[String]()

    Option(params.generatorType) match {
      case Some(t) if !generatorTypes.contains(t) =>
        errors += s"Unknown 'generator-type' provided. Must be one of: $generatorTypes"
      case None =>
        errors += s"'generator-type' is required"
      case _ =>
    }

    if (params.generatorType != "local") {
      Option(initialData.service) match {
        case Some(s) if s.isEmpty =>
          errors += s"Generator 'service' can not be empty"
        case None =>
          errors += s"Generator 'service' is required for non-'local' generator-type"
        case _ =>
          if (initialData.service contains "://") {
            val generatorUrl = new URI(initialData.service)
            if (!generatorUrl.getScheme.equals("service-zk")) {
              errors += s"Generator 'service' uri protocol prefix must be 'service-zk://'. Or use plain service name instead"
            }
          }
          if (params.service == null) {
            errors += s"Unknown generator 'service' provided"
          } else {
            if (params.service.serviceType != "ZKCoord") {
              errors += s"Provided 'service' is not of type ZKCoord"
            }
          }
      }
    }
    else {
      if (Option(initialData.service).isDefined) {
        errors += s"Generator 'service' must be null for 'local' generator-type"
      }
    }

    if (params.generatorType != "local") {
      if (params.instanceCount <= 0) {
        errors += s"Generator 'instance-count' must be a positive integer for non-'local' generator-type"
      }
    }
    else {
      if (params.instanceCount > 0) {
        errors += s"Generator 'instance-count' must be null or 0 for 'local' generator-type"
      }
    }

    errors
  }
}
