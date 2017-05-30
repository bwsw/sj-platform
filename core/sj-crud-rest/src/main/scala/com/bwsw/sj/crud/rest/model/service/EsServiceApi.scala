package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.ESService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

class EsServiceApi(name: String,
                   val index: String,
                   val provider: String,
                   description: Option[String] = Some(RestLiterals.defaultDescription),
                   @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.elasticsearchType))
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.elasticsearchType), name, description) {

  override def to()(implicit injector: Injector): ESService = {
    val modelService =
      new ESService(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        provider = this.provider,
        index = this.index,
        serviceType = this.serviceType.getOrElse(ServiceLiterals.elasticsearchType)
      )

    modelService
  }
}
