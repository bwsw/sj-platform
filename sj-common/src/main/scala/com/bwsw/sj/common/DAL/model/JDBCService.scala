package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

/**
  * Created: 23/05/2016
  *
  * @author Kseniya Tomskikh
  */
class JDBCService() extends Service {

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

}
