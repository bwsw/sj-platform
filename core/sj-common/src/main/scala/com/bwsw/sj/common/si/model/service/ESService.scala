package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service.ESServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.{validateNamespace, validateProvider}
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable.ArrayBuffer

class ESService(name: String,
                val index: String,
                val provider: String,
                description: String,
                serviceType: String)
  extends Service(serviceType, name, description) {

  override def asService(): ESServiceDomain = {
    val providerRepository = ConnectionRepository.getProviderRepository

    val modelService =
      new ESServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        index = this.index
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'index' field
    Option(this.index) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Index")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Index")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "index", x)
          }
        }
    }

    errors
  }
}
