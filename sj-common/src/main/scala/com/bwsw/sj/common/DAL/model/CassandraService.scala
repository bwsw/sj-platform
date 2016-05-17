package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class CassandraService() extends Service {
  @Reference var provider: Provider = null
  var keyspace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, keyspace: String) = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.keyspace = keyspace
  }
}
