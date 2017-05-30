package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.KafkaService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

class KfkQServiceApi(name: String,
                     val provider: String,
                     val zkProvider: String,
                     val zkNamespace: String,
                     description: Option[String] = Some(RestLiterals.defaultDescription),
                     @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.kafkaType))
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.kafkaType), name, description) {

  override def to()(implicit injector: Injector): KafkaService = {
    val modelService =
      new KafkaService(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        provider = this.provider,
        zkProvider = this.zkProvider,
        zkNamespace = this.zkNamespace,
        serviceType = this.serviceType.getOrElse(ServiceLiterals.kafkaType)
      )

    modelService
  }
}
