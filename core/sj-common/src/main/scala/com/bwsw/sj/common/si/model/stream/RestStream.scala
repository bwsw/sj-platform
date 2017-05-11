package com.bwsw.sj.common.si.model.stream

import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.dal.model.stream.RestStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}

import scala.collection.mutable.ArrayBuffer

class RestStream(name: String,
                 service: String,
                 tags: Array[String],
                 force: Boolean,
                 streamType: String,
                 description: String)
  extends SjStream(streamType, name, service, tags, force, description) {

  override def to(): RestStreamDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository

    new RestStreamDomain(
      name,
      serviceRepository.get(service).get.asInstanceOf[RestServiceDomain],
      description,
      force,
      tags)
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralFields()

    Option(service) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Service")
      case Some(x) =>
        val serviceDAO = ConnectionRepository.getServiceRepository
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

    errors
  }
}
