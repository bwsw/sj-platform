package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.service.AerospikeService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

class ArspkDBServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.aerospikeType
  var namespace: String = null
  var provider: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new AerospikeService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.namespace
    )

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'namespace' field
    Option(this.namespace) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Namespace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Namespace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "namespace", x)
          }
        }
    }

    errors
  }
}
