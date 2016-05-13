package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class ESService() extends Service {
  @Reference var provider: Provider = null
  var index: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, index: String) = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.index = index
  }
}
