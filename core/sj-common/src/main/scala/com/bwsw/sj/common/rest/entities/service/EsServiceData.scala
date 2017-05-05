package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.service.ESService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class EsServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.elasticsearchType
  var provider: String = null
  var index: String = null

  override def asModelService(): ESService = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new ESService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.index
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
