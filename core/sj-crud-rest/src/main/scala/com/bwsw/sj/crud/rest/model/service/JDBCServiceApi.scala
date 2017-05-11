package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.JDBCService
import com.bwsw.sj.common.utils.ServiceLiterals
import com.fasterxml.jackson.annotation.JsonProperty

class JDBCServiceApi(name: String,
                     val database: String,
                     val provider: String,
                     description: String = "No description",
                     @JsonProperty("type") serviceType: String = ServiceLiterals.jdbcType)
  extends ServiceApi(serviceType, name, description) {

  override def asService(): JDBCService = {
    val modelService =
      new JDBCService(
        name = this.name,
        description = this.description,
        provider = this.provider,
        database = this.database,
        serviceType = this.serviceType
      )

    modelService
  }
}
