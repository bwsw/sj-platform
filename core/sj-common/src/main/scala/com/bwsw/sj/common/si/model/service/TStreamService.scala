package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.rest.utils.ValidationUtils.{validatePrefix, validateProvider, validateToken}
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class TStreamService(name: String,
                     val provider: String,
                     val prefix: String,
                     val token: String,
                     description: String,
                     serviceType: String)
                    (implicit injector: Injector)
  extends Service(serviceType, name, description) {

  override def to(): TStreamServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository

    val modelService =
      new TStreamServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        prefix = this.prefix,
        token = this.token
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = connectionRepository.getProviderRepository

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
          validatePrefix(x).foreach(error => errors += createMessage("entity.error.incorrect.service.prefix", x, error))
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
