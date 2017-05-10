package com.bwsw.sj.common.rest.model.stream

import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.dal.model.stream.RestStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}

import scala.collection.mutable.ArrayBuffer

/**
  * @author Pavel Tomskikh
  */
class RestStreamApi extends StreamApi {

  streamType = StreamLiterals.restOutputType

  override def validate(): ArrayBuffer[String] = {
    val serviceDAO = ConnectionRepository.getServiceRepository
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

  override def asModelStream(): RestStreamDomain = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val modelStream = new RestStreamDomain(
      this.name,
      serviceDAO.get(this.service).get.asInstanceOf[RestServiceDomain],
      this.description,
      this.force,
      this.tags
    )

    modelStream
  }
}
