package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.service.CassandraService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class CassDBServiceData(
                         override val name: String,
                         val provider: String,
                         val keyspace: String,
                         override val description: String = ServiceLiterals.defaultDescription)
  extends ServiceData(ServiceLiterals.cassandraType, name, description) {

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new CassandraService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.keyspace
    )

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
        errors += createMessage("entity.error.attribute.required", "Keyspace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Keyspace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "keyspace", x)
          }
        }
    }

    errors
  }
}
