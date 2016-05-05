package com.bwsw.sj.crud.rest.validator.stream

import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.crud.rest.entities._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mendelbaum_nm
  */
class StreamValidator {
  import com.bwsw.sj.common.StreamConstants._

  var streamDAO: GenericMongoService[SjStream] = null

  /**
    * Validating input parameters for stream
    *
    * @param params - parameters of stream being validated
    * @param initialData - input parameters for stream being validated
    * @return - List of errors
    */
  def validate(params: SjStream, initialData: SjStreamData) = {
    streamDAO = ConnectionRepository.getStreamService
    val errors = new ArrayBuffer[String]()

    Option(params.name) match {
      case Some(n) if n.isEmpty =>
        errors += s"'name' can not be empty"
      case None =>
        errors += s"'name' is required"
      case _ =>
    }

    val stream = streamDAO.get(params.name)
    if (stream != null) {
      errors += s"Stream with name ${params.name} already exists"
    }

    Option(params.description) match {
      case Some(d) if d.isEmpty =>
        errors += s"'description' can not be empty"
      case None =>
        errors += s"'description' is required"
      case _ =>
    }


    params.partitions match {
      case partitions: Int =>
        if (partitions <= 0) {
          errors += s"'partitions' must be a positive integer"
        }
      case _ =>
        errors += "Unknown type of 'partitions' parameter. Must be Int"
    }

    Option(initialData.service) match {
      case Some(s) if s.isEmpty =>
        errors += s"'service' can not be empty"
      case Some(s) if params.service == null =>
        errors += s"Unknown 'service' provided"
      case None =>
        errors += s"'service' is required"
      case _ =>
    }

    Option(params.streamType) match {
      case Some(t) if !streamTypes.contains(t) =>
        errors += s"Unknown 'streamType' provided. Must be one of: $streamTypes"
      case None =>
        errors += s"'streamType' is required"
      case _ =>
        if (params.streamType == "Tstream") {
          val validatorClass = Class.forName("com.bwsw.sj.crud.rest.validator.stream.GeneratorValidator")
          val validator = validatorClass.newInstance().asInstanceOf[GeneratorValidator]
          errors ++= validator.validate(params.generator, initialData.generator)
        }
    }

    Option(params.tags) match {
      case Some(t) if t.isEmpty =>
        errors += s"'tags' can not be empty"
      case None =>
        errors += s"'tags' is required"
      case _ =>
    }

    errors
  }
}
