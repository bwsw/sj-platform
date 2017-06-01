package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.CassandraService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

class CassDBServiceApi(name: String,
                       val keyspace: String,
                       provider: String,
                       description: Option[String] = Some(RestLiterals.defaultDescription),
                       @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.cassandraType))
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.cassandraType), name, provider, description) {

  override def to()(implicit injector: Injector): CassandraService = {
    val modelService =
      new CassandraService(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        provider = this.provider,
        keyspace = this.keyspace,
        serviceType = this.serviceType.getOrElse(ServiceLiterals.cassandraType)
      )

    modelService
  }
}
