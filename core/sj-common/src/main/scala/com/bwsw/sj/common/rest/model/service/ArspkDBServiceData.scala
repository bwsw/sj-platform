package com.bwsw.sj.common.rest.model.service

import com.bwsw.sj.common.dal.model.service.AerospikeService
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._

class ArspkDBServiceData(
                          override val name: String,
                          val namespace: String,
                          val provider: String,
                          override val description: String = ServiceLiterals.defaultDescription)
  extends ServiceData(ServiceLiterals.aerospikeType, name, description) {

  override def asModelService(): AerospikeService = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new AerospikeService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.namespace
    )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
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
