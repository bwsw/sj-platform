package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{PropertyField, ReferenceField}
import com.bwsw.sj.common.utils.ServiceLiterals

class KafkaServiceDomain(override val name: String,
                         override val description: String,
                         @ReferenceField override val provider: ProviderDomain,
                         @ReferenceField(value = "zk-provider") val zkProvider: ProviderDomain,
                         @PropertyField("zk-namespace") val zkNamespace: String)
  extends ServiceDomain(name, description, provider, ServiceLiterals.kafkaType) {

}
