package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.CassandraService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class CassDBServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.cassandraType
  var provider: String = null
  var keyspace: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new CassandraService()
    super.fillModelService(modelService)
    modelService.provider = providerDAO.get(this.provider).get
    modelService.keyspace = this.keyspace

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'keyspace' field
    errors ++= validateStringFieldRequired(this.keyspace, "Keyspace")
    if (!validateNamespace(this.keyspace)) {
      errors += s"Service has incorrect 'keyspace': '$keyspace'. " +
        s"Name must be contain digits, lowercase letters or underscore. First symbol must be a letter"
    }

    errors
  }
}
