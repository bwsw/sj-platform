package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.{PropertyField, ReferenceField}
import com.bwsw.sj.common.rest.DTO.service.{KfkQServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

class KafkaService(override val name: String,
                   override val description: String,
                   @ReferenceField val provider: Provider,
                   @ReferenceField(value = "zk-provider") val zkProvider: Provider,
                   @PropertyField("zk-namespace") val zkNamespace: String,
                   override val serviceType: String = ServiceLiterals.kafkaType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData = {
    val protocolService = new KfkQServiceData()
    super.fillProtocolService(protocolService)

    protocolService.provider = this.provider.name
    protocolService.zkProvider = this.zkProvider.name
    protocolService.zkNamespace = this.zkNamespace

    protocolService
  }
}
