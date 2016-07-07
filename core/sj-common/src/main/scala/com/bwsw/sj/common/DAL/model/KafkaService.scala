package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class KafkaService() extends Service {
  @Reference var provider: Provider = null
  @Reference var zkProvider: Provider = null
  var zkNamespace: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, zkProvider: Provider, zkNamespace: String) = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.zkProvider = zkProvider
    this.zkNamespace = zkNamespace
  }
}
