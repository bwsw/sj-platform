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
    Option(this.keyspace) match {
      case None =>
        errors += "'Keyspace' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Keyspace' is required"
        }
        else {
          if (!validateNamespace(x)) {
            errors += s"Service has incorrect 'keyspace': '$x'. " +
              s"Name must be contain digits, lowercase letters or underscore. First symbol must be a letter"
          }
        }
    }

    errors
  }
}
