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
  var namespace: String = null
  var login: String = null
  var password: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, namespace: String, login: String, password: String) = {
    this()
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.namespace = namespace
    this.login = login
    this.password = password
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new JDBCServiceData()
    super.fillProtocolService(protocolService)

    protocolService.namespace = this.namespace
    protocolService.provider = this.provider.name
    protocolService.login = this.login
    protocolService.password = this.password

    protocolService
  }
}
