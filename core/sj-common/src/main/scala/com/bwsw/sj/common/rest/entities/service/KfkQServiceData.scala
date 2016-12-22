package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.KafkaService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ProviderLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable.ArrayBuffer

class KfkQServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.kafkaType
  var provider: String = null
  @JsonProperty("zk-provider") var zkProvider: String = null
  @JsonProperty("zk-namespace") var zkNamespace: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new KafkaService()
    super.fillModelService(modelService)
    modelService.provider = providerDAO.get(this.provider).get
    modelService.zkProvider = providerDAO.get(this.zkProvider).get
    modelService.zkNamespace = this.zkNamespace

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'zkProvider' field
    Option(this.zkProvider) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Zk-provider")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Zk-provider")
        }
        else {
          val zkProviderObj = providerDAO.get(x)
          zkProviderObj match {
            case None =>
              errors += createMessage("entity.error.doesnot.exist", "Zookeeper provider", x)
            case Some(zkProviderFormDB) =>
              if (zkProviderFormDB.providerType != ProviderLiterals.zookeeperType) {
                errors += createMessage("entity.error.must.one.type.other.given", "Zk-provider", ProviderLiterals.zookeeperType, zkProviderFormDB.providerType)
              }
          }
        }
    }

    // 'zkNamespace' field
    Option(this.zkNamespace) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Zk-namespace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Zk-namespace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "zk-namespace", x)
          }
        }
    }

    errors
  }
}
