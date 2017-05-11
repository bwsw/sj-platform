package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.TStreamService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class TstrQServiceApi(name: String,
                      val provider: String,
                      val prefix: String,
                      val token: String,
                      description: String = RestLiterals.defaultDescription,
                      @JsonProperty("type") serviceType: String = ServiceLiterals.tstreamsType)
  extends ServiceApi(serviceType, name, description) {

  override def to(): TStreamService = {
    val modelService =
      new TStreamService(
        name = this.name,
        description = this.description,
        provider = this.provider,
        prefix = this.prefix,
        token = this.token,
        serviceType = this.serviceType
      )

    modelService
  }
}
