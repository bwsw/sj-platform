package com.bwsw.sj.crud.rest.validator.stream


import java.net.URI

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mendelbaum_nm on 05.05.16.
  */

class GeneratorValidator {
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
        errors += s"Unknown 'generatorType' provided. Must be one of: $generatorTypes"
      case None =>
        errors += s"'streamType' is required"
      case _ =>
    }


    Option(initialData.service) match {
      case Some(s) if s.isEmpty=>
        errors += s"Generator 'service' can not be empty"
      case None =>
        errors += s"Generator 'streamType' is required"
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

    params.instanceCount match {
      case partitions: Int =>
        if (partitions <= 0) {
          errors += s"Generator 'instanceCount' must be a positive integer"
        }
      case _ =>
        errors += "Unknown type of 'instanceCount' parameter. Must be Int"
    }

    errors
  }
}
