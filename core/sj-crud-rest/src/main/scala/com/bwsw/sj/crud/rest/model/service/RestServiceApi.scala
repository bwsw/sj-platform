package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.RestService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * @author Pavel Tomskikh
  */
class RestServiceApi(name: String,
                     val provider: String,
                     val basePath: String = "/",
                     val httpVersion: String = RestLiterals.http_1_1,
                     val headers: Map[String, String] = Map(),
                     description: String = RestLiterals.defaultDescription,
                     @JsonProperty("type") serviceType: String = ServiceLiterals.restType)
  extends ServiceApi(serviceType, name, description) {

  override def to(): RestService = {
    val modelService =
      new RestService(
        name = this.name,
        description = this.description,
        provider = this.provider,
        basePath = this.basePath,
        httpVersion = this.httpVersion,
        headers = this.headers,
        serviceType = this.serviceType
      )

    modelService
  }
}
