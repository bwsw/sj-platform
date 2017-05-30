package com.bwsw.sj.crud.rest.model.provider

import com.bwsw.sj.common.si.model.provider.JDBCProvider
import com.bwsw.sj.common.utils.{ProviderLiterals, RestLiterals}
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import scaldi.Injector


class JDBCProviderApi(name: String,
                      login: String,
                      password: String,
                      hosts: Array[String],
                      val driver: String,
                      description: Option[String] = Some(RestLiterals.defaultDescription),
                      @JsonProperty("type") providerType: Option[String] = Some(ProviderLiterals.jdbcType))
  extends ProviderApi(name, login, password, providerType.getOrElse(ProviderLiterals.jdbcType), hosts, description) {

  @JsonIgnore
  override def to()(implicit injector: Injector): JDBCProvider = {
    val provider =
      new JDBCProvider(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        hosts = this.hosts,
        login = this.login,
        password = this.password,
        driver = this.driver,
        providerType = this.providerType.getOrElse(ProviderLiterals.jdbcType)
      )

    provider
  }
}
