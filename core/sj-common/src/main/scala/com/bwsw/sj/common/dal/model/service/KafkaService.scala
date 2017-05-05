package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.Provider
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{PropertyField, ReferenceField}
import com.bwsw.sj.common.rest.model.service.{KfkQServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

class KafkaService(override val name: String,
                   override val description: String,
                   @ReferenceField val provider: Provider,
                   @ReferenceField(value = "zk-provider") val zkProvider: Provider,
                   @PropertyField("zk-namespace") val zkNamespace: String,
                   override val serviceType: String = ServiceLiterals.kafkaType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData =
    new KfkQServiceData(name, provider.name, zkProvider.name, zkNamespace, description)
}
