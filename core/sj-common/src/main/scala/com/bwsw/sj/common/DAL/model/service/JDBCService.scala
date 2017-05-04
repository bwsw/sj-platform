package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.JDBCProvider
import com.bwsw.sj.common.rest.entities.service.{JDBCServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals
import org.mongodb.morphia.annotations.Reference

/**
  *
  * @author Kseniya Tomskikh
  */
class JDBCService() extends Service {
  serviceType = ServiceLiterals.jdbcType
  @Reference var provider: JDBCProvider = null

  var database: String = null

  def this(name: String, serviceType: String, description: String, provider: JDBCProvider, database: String) = {
    this
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.database = database
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new JDBCServiceData()
    super.fillProtocolService(protocolService)
    protocolService.provider = this.provider.name
    protocolService.database = this.database
    protocolService
  }
}
