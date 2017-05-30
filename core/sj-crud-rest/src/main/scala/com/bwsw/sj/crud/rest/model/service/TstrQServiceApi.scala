package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.TStreamService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

class TstrQServiceApi(name: String,
                      val provider: String,
                      val prefix: String,
                      val token: String,
                      description: Option[String] = Some(RestLiterals.defaultDescription),
                      @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.tstreamsType))
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.tstreamsType), name, description) {

  override def to()(implicit injector: Injector): TStreamService = {
    val modelService =
      new TStreamService(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        provider = this.provider,
        prefix = this.prefix,
        token = this.token,
        serviceType = this.serviceType.getOrElse(ServiceLiterals.tstreamsType)
      )

    modelService
  }
}
