package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.AerospikeService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

class ArspkDBServiceApi(name: String,
                        val namespace: String,
                        val provider: String,
                        description: Option[String] = Some(RestLiterals.defaultDescription),
                        @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.aerospikeType))
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.aerospikeType), name, description) {

  override def to()(implicit injector: Injector): AerospikeService = {
    val modelService =
      new AerospikeService(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        provider = this.provider,
        namespace = this.namespace,
        serviceType = this.serviceType.getOrElse(ServiceLiterals.aerospikeType)
      )

    modelService
  }
}
