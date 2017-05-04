package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.stream.RestSjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}

import scala.collection.mutable.ArrayBuffer

/**
  * @author Pavel Tomskikh
  */
class RestStreamData extends StreamData {

  streamType = StreamLiterals.restOutputType

  override def validate(): ArrayBuffer[String] = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    Option(this.service) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Service")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Service")
        } else {
          val serviceObj = serviceDAO.get(x)
          serviceObj match {
            case None =>
              errors += createMessage("entity.error.doesnot.exist", "Service", x)
            case Some(someService) =>
              if (someService.serviceType != ServiceLiterals.restType) {
                errors += createMessage(
                  "entity.error.must.one.type.other.given",
                  s"Service for '${StreamLiterals.restOutputType}' stream",
                  ServiceLiterals.restType,
                  someService.serviceType)
              }
          }
        }
    }

    errors
  }

  override def asModelStream(): RestSjStream = {
    val modelStream = new RestSjStream
    fillModelStream(modelStream)

    modelStream
  }
}
