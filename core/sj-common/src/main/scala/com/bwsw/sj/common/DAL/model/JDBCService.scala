package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.service.{JDBCServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals
import org.mongodb.morphia.annotations.Reference

/**
  *
  * @author Kseniya Tomskikh
  */
class JDBCService() extends Service {
  serviceType = ServiceLiterals.jdbcType
  @Reference var provider: Provider = null
  var driver: String = null

  var database: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, driver: String, database: String) = {
    this
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.driver = driver
    this.database = database
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new JDBCServiceData()
    super.fillProtocolService(protocolService)
    protocolService.provider = this.provider.name
    protocolService.driver = this.driver
    protocolService.database = this.database
    protocolService
  }
}
