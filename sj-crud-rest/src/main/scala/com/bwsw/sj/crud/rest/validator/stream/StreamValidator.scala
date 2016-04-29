package com.bwsw.sj.crud.rest.validator.stream

import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.crud.rest.entities._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mendelbaum_nm on 29.04.16.
  */
class StreamValidator {

  var streamDAO: GenericMongoService[SjStream] = null

  /**
    * Validating input parameters for stream
    *
    * @param parameters - input parameters for stream being validated
    * @return - List of errors
    */
  def validate(parameters: SjStream) = {
    streamDAO = ConnectionRepository.getStreamService
    val errors = new ArrayBuffer[String]()

    val stream = streamDAO.get(parameters.name)
    if (stream != null) {
      errors += s"Stream with name ${parameters.name} already exists."
    }
    errors
  }
}
