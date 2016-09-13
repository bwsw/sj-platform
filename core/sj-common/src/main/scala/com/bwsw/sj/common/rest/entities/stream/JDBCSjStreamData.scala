package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.JDBCSjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceConstants, StreamConstants}

import scala.collection.mutable.ArrayBuffer

class JDBCSjStreamData() extends SjStreamData() {
  streamType = StreamConstants.jdbcOutputType

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
            if (service.serviceType != ServiceConstants.jdbcServiceType) {
              errors += s"Service for ${StreamConstants.jdbcOutputType} stream " +
                s"must be of '${ServiceConstants.jdbcServiceType}' type ('${service.serviceType}' is given instead)"
            }
        }
    }

    errors
  }

   override def asModelStream() = {
    val modelStream = new JDBCSjStream()
    super.fillModelStream(modelStream)

    modelStream
  }
}
