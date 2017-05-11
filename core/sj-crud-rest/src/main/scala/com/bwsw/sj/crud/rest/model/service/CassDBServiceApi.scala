package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.CassandraService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class CassDBServiceApi(name: String,
                       val keyspace: String,
                       val provider: String,
                       description: String = RestLiterals.defaultDescription,
                       @JsonProperty("type") serviceType: String = ServiceLiterals.cassandraType)
  extends ServiceApi(serviceType, name, description) {

  override def to(): CassandraService = {
    val modelService =
      new CassandraService(
        name = this.name,
        description = this.description,
        provider = this.provider,
        keyspace = this.keyspace,
        serviceType = this.serviceType
      )

    modelService
  }
}
