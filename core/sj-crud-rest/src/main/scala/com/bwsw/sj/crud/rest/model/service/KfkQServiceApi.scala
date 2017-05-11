package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.KafkaService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class KfkQServiceApi(name: String,
                     val provider: String,
                     val zkProvider: String,
                     val zkNamespace: String,
                     description: String = RestLiterals.defaultDescription,
                     @JsonProperty("type") serviceType: String = ServiceLiterals.kafkaType)
  extends ServiceApi(serviceType, name, description) {

  override def to(): KafkaService = {
    val modelService =
      new KafkaService(
        name = this.name,
        description = this.description,
        provider = this.provider,
        zkProvider = this.zkProvider,
        zkNamespace = this.zkNamespace,
        serviceType = this.serviceType
      )

    modelService
  }
}
