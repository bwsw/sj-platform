package com.bwsw.sj.common.rest.model.service

import com.bwsw.sj.common.dal.model.service.KafkaService
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ProviderLiterals, ServiceLiterals}

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._

class KfkQServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.kafkaType
  var provider: String = null
  var zkProvider: String = null
  var zkNamespace: String = null

  override def asModelService(): KafkaService = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new KafkaService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      providerDAO.get(this.zkProvider).get,
      this.zkNamespace
    )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'zkProvider' field
    Option(this.zkProvider) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "zkProvider")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "zkProvider")
        }
        else {
          val zkProviderObj = providerDAO.get(x)
          zkProviderObj match {
            case None =>
              errors += createMessage("entity.error.doesnot.exist", "Zookeeper provider", x)
            case Some(zkProviderFormDB) =>
              if (zkProviderFormDB.providerType != ProviderLiterals.zookeeperType) {
                errors += createMessage("entity.error.must.one.type.other.given", "zkProvider", ProviderLiterals.zookeeperType, zkProviderFormDB.providerType)
              }
          }
        }
    }

    // 'zkNamespace' field
    Option(this.zkNamespace) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "zkNamespace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "zkNamespace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "zkNamespace", x)
          }
        }
    }

    errors
  }
}
