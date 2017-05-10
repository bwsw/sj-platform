package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.ESService
import com.bwsw.sj.common.utils.ServiceLiterals
import com.fasterxml.jackson.annotation.JsonProperty

class EsServiceApi(name: String,
                   val index: String,
                   val provider: String,
                   description: String = "No description",
                   @JsonProperty("type") serviceType: String = ServiceLiterals.elasticsearchType)
  extends ServiceApi(serviceType, name, description) {

  override def asService(): ESService = {
    val modelService =
      new ESService(
        name = this.name,
        description = this.description,
        provider = this.provider,
        index = this.index,
        serviceType = this.serviceType
      )

    modelService
  }
}
