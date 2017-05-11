package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.{validateNamespace, validateProvider}
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.ProviderLiterals

import scala.collection.mutable.ArrayBuffer

class KafkaService(name: String,
                   val provider: String,
                   val zkProvider: String,
                   val zkNamespace: String,
                   description: String,
                   serviceType: String)
  extends Service(serviceType, name, description) {

  override def to(): KafkaServiceDomain = {
    val providerRepository = ConnectionRepository.getProviderRepository

    val modelService =
      new KafkaServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        zkProvider = providerRepository.get(this.zkProvider).get,
        zkNamespace = this.zkNamespace
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val providerRepository = ConnectionRepository.getProviderRepository

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
          val zkProviderObj = providerRepository.get(x)
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
