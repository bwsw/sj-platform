package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.JDBCService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class JDBCServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.jdbcType
  var namespace: String = null
  var provider: String = null
  var driver: String = null
  var databaseName: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new JDBCService()
    super.fillModelService(modelService)
    modelService.provider = providerDAO.get(this.provider).get
    modelService.namespace = this.namespace
    modelService.driver = this.driver
    modelService.databaseName = this.databaseName

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'namespace'
    Option(this.namespace) match {
      case None =>
        errors += "'Namespace' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Namespace' is required"
        }
        else {
          if (!validateNamespace(x)) {
            errors += s"Service has incorrect 'namespace': '$x'. " +
              s"Name must be contain digits, lowercase letters or underscore. First symbol must be a letter"
          }
        }
    }

    errors
  }
}
