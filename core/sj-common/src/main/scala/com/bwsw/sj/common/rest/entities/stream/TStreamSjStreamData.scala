package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceConstants, StreamConstants}

import scala.collection.mutable.ArrayBuffer

class TStreamSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.tStreamType
  var partitions: Int = 0
  var generator: GeneratorData = null

  override def validate() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    Option(this.service) match {
      case None =>
        errors += s"'Service' is required"
      case Some(x) =>
        val serviceObj = serviceDAO.get(this.service)
        serviceObj match {
          case None =>
            errors += s"Service '${this.service}' does not exist"
          case Some(service) =>
            if (service.serviceType != ServiceConstants.tstreamsServiceType) {
              errors += s"Service for ${StreamConstants.tStreamType} stream " +
                s"must be of '${ServiceConstants.tstreamsServiceType}' type ('${service.serviceType}' is given instead)"
            }
        }
    }

    //partitions
    if (this.partitions <= 0)
      errors += s"'Partitions' must be a positive integer"

    //generator
    if (this.generator == null) {
      errors += s"'Generator' is required"
    }
    else {
      errors ++= this.generator.validate()
    }

    errors
  }

  override def asModelStream() = {
    val modelStream = new TStreamSjStream()
    super.fillModelStream(modelStream)
    modelStream.partitions = this.partitions
    modelStream.generator = this.generator.asModelGenerator()

    modelStream
  }
}
