package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.ZKService
import com.bwsw.sj.common.utils.ServiceLiterals
import com.fasterxml.jackson.annotation.JsonProperty

class ZKCoordServiceApi(name: String,
                        val provider: String,
                        val namespace: String,
                        description: String = "No description",
                        @JsonProperty("type") serviceType: String = ServiceLiterals.zookeeperType)
  extends ServiceApi(serviceType, name, description) {

  override def asService(): ZKService = {
    val modelService =
      new ZKService(
        name = this.name,
        provider = this.provider,
        namespace = this.namespace,
        description = this.description,
        serviceType = this.serviceType
      )

    modelService
  }
}
