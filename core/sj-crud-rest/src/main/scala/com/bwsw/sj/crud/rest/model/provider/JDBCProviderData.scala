package com.bwsw.sj.crud.rest.model.provider

import com.bwsw.sj.common.dal.model.provider.JDBCProvider
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}


class JDBCProviderData(name: String,
                       login: String,
                       password: String,
                       @JsonProperty("type") providerType: String,
                       hosts: Array[String],
                       val driver: String,
                       description: String = "No description"
                      ) extends ProviderData(name, login, password, providerType, hosts, description) {

  @JsonIgnore
  override def asModelProvider(): JDBCProvider = {
    val provider = new JDBCProvider(
      this.name,
      this.description,
      this.hosts,
      this.login,
      this.password,
      this.driver,
      this.providerType
    )

    provider
  }
}
