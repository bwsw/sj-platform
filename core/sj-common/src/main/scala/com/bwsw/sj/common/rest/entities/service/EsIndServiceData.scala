package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.ESService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class EsIndServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.elasticsearchType
  var provider: String = null
  var index: String = null
  var login: String = null
  var password: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new ESService()
    super.fillModelService(modelService)
    modelService.provider = providerDAO.get(this.provider).get
    modelService.index = this.index
    modelService.login = this.login
    modelService.password = this.password

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'index' field
    errors ++= validateStringFieldRequired(this.index, "Index")
    if (!validateNamespace(this.index)) {
      errors += s"Service has incorrect 'index': '$index'. " +
        s"Name must be contain digits, lowercase letters or underscore. First symbol must be a letter"
    }

    errors
  }
}
