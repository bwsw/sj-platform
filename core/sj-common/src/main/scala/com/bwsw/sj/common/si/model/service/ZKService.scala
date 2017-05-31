package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.rest.utils.ValidationUtils.{validateNamespace, validateProvider}
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class ZKService(name: String,
                val provider: String,
                val namespace: String,
                description: String,
                serviceType: String)
               (implicit injector: Injector)
  extends Service(serviceType, name, description) {

  import messageResourceUtils.createMessage

  override def to(): ZKServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository

    val modelService =
      new ZKServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        namespace = this.namespace
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
            //todo think about using, maybe this is going to be more correct to check with validatePrefix()
          }
        }
    }

    errors
  }
}
