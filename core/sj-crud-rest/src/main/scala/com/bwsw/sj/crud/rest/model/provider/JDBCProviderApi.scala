package com.bwsw.sj.crud.rest.model.provider

import com.bwsw.sj.common.si.model.provider.JDBCProvider
import com.bwsw.sj.common.utils.{ProviderLiterals, RestLiterals}
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}


class JDBCProviderApi(name: String,
                      login: String,
                      password: String,
                      hosts: Array[String],
                      val driver: String,
                      description: String = RestLiterals.defaultDescription,
                      @JsonProperty("type") providerType: String = ProviderLiterals.jdbcType)
  extends ProviderApi(name, login, password, providerType, hosts, description) {

  @JsonIgnore
  override def to(): JDBCProvider = {
    val provider =
      new JDBCProvider(
        name = this.name,
        description = this.description,
        hosts = this.hosts,
        login = this.login,
        password = this.password,
        driver = this.driver,
        providerType = this.providerType
      )

    provider
  }
}
