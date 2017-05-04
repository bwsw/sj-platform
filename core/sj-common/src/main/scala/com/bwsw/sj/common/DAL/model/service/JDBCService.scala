package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.JDBCProvider
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.DTO.service.{JDBCServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

/**
  *
  * @author Kseniya Tomskikh
  */
class JDBCService(override val name: String,
                  override val description: String,
                  @ReferenceField val provider: JDBCProvider,
                  val database: String,
                  override val serviceType: String = ServiceLiterals.jdbcType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData = {
    val protocolService = new JDBCServiceData()
    super.fillProtocolService(protocolService)
    protocolService.provider = this.provider.name
    protocolService.database = this.database
    protocolService
  }
}
