package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.KafkaService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceLiterals, ProviderLiterals}
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
        errors += s"'Zk-provider' is required"
      case Some(p) =>
        val zkProviderObj = providerDAO.get(p)
        zkProviderObj match {
          case Some(zkProvider) =>
            if (zkProvider.providerType != ProviderLiterals.zookeeperType) {
              errors += s"'Zk-provider' must be of type '${ProviderLiterals.zookeeperType}' " +
                s"('${zkProvider.providerType}' is given instead)"
            }
          case None => errors += s"Zookeeper provider '$p' does not exist"
        }
    }

    // 'zkNamespace' field
    errors ++= validateStringFieldRequired(this.zkNamespace, "ZK-namespace")
    if (!validateNamespace(this.zkNamespace)) {
      errors += s"Service has incorrect 'zk-namespace': '$zkNamespace'. " +
        s"Name must be contain digits, lowercase letters or underscore. First symbol must be a letter"
    }

    errors
  }
}
