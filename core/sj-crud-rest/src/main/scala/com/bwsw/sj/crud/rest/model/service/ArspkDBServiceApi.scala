package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.AerospikeService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class ArspkDBServiceApi(name: String,
                        val namespace: String,
                        val provider: String,
                        description: String = RestLiterals.defaultDescription,
                        @JsonProperty("type") serviceType: String = ServiceLiterals.aerospikeType)
  extends ServiceApi(serviceType, name, description) {

  override def to(): AerospikeService = {
    val modelService =
      new AerospikeService(
        name = this.name,
        description = this.description,
        provider = this.provider,
        namespace = this.namespace,
        serviceType = this.serviceType
      )

    modelService
  }
}
