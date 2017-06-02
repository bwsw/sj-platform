package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.RestService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

/**
  * @author Pavel Tomskikh
  */
class RestServiceApi(name: String,
                     provider: String,
                     val basePath: Option[String] = Some("/"),
                     val httpVersion: Option[String] = Some(RestLiterals.http_1_1),
                     val headers: Option[Map[String, String]] = Some(Map()),
                     description: Option[String] = Some(RestLiterals.defaultDescription),
                     @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.restType))
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.restType), name, provider, description) {

  override def to()(implicit injector: Injector): RestService = {
    val modelService =
      new RestService(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        provider = this.provider,
        basePath = this.basePath.getOrElse("/"),
        httpVersion = this.httpVersion.getOrElse(RestLiterals.http_1_1),
        headers = this.headers.getOrElse(Map()),
        serviceType = this.serviceType.getOrElse(ServiceLiterals.restType)
      )

    modelService
  }
}
