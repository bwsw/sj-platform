package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class AerospikeService() extends Service {
  @Reference var provider: Provider = null
  var namespace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, namespace: String) = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.namespace = namespace
  }
}
