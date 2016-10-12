package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.JDBCService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class JDBCServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.jdbcType
  var namespace: String = null
  var provider: String = null
  var login: String = null
  var password: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new JDBCService()
    super.fillModelService(modelService)
    modelService.provider = providerDAO.get(this.provider).get
    modelService.namespace = this.namespace
    modelService.login = this.login
    modelService.password = this.password

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'namespace'
    errors ++= validateStringFieldRequired(this.namespace, "Namespace")
    if (!validateNamespace(this.namespace)) {
      errors += s"Service has incorrect 'namespace': '$namespace'. " +
        s"Name must be contain digits, lowercase letters or underscore. First symbol must be a letter"
    }

    errors
  }
}
