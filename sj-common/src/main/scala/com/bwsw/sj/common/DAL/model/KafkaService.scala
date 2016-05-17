package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class KafkaService() extends Service {
  @Reference var provider: Provider = null

  def this(name: String, serviceType: String, description: String, provider: Provider) = {
    this()
    this.name =name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
  }
}
