package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.service.TStreamService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class TstrQServiceData(
                        override val name: String,
                        val provider: String,
                        val prefix: String,
                        val token: String,
                        override val description: String = ServiceLiterals.defaultDescription)
  extends ServiceData(ServiceLiterals.tstreamsType, name, description) {

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new TStreamService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.prefix,
      this.token
    )

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'prefix' field
    Option(this.prefix) match {
      case None =>

        errors += createMessage("entity.error.attribute.required", "Prefix")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Prefix")
        }
        else {
          if (!validatePrefix(x)) {
            errors += createMessage("entity.error.incorrect.service.prefix", x)
          }
        }
    }

    // 'token' field
    Option(this.token) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Token")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Token")
        }
        else {
          if (!validateToken(x)) {
            errors += createMessage("entity.error.incorrect.service.token", x)
          }
        }
    }

    errors
  }
}
