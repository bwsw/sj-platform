package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.JDBCService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

class JDBCServiceApi(name: String,
                     val database: String,
                     val provider: String,
                     description: Option[String] = Some(RestLiterals.defaultDescription),
                     @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.jdbcType))
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.jdbcType), name, description) {

  override def to()(implicit injector: Injector): JDBCService = {
    val modelService =
      new JDBCService(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        provider = this.provider,
        database = this.database,
        serviceType = this.serviceType.getOrElse(ServiceLiterals.jdbcType)
      )

    modelService
  }
}
