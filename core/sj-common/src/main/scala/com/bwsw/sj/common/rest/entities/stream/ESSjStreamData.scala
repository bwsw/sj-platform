package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.ESSjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}

import scala.collection.mutable.ArrayBuffer

class ESSjStreamData() extends SjStreamData() {
  streamType = StreamLiterals.esOutputType

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
            if (service.serviceType != ServiceLiterals.elasticsearchType) {
              errors += s"Service for ${StreamLiterals.esOutputType} stream " +
                s"must be of '${ServiceLiterals.elasticsearchType}' type ('${service.serviceType}' is given instead)"
            }
        }
    }

    errors
  }

  override def asModelStream() = {
    val modelStream = new ESSjStream()
    super.fillModelStream(modelStream)

    modelStream
  }
}
