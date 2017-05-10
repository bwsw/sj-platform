package com.bwsw.sj.crud.rest.model.provider

import com.bwsw.sj.common.si.model.provider.{JDBCProvider, Provider}
import com.bwsw.sj.common.utils.ProviderLiterals
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[ProviderApi], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[JDBCProviderApi], name = ProviderLiterals.jdbcType)
))
class ProviderApi(val name: String,
                  val login: String,
                  val password: String,
                  @JsonProperty("type") val providerType: String,
                  val hosts: Array[String],
                  val description: String = "No description") {
  @JsonIgnore
  def asProvider(): Provider = {
    val provider =
      new Provider(
        name = this.name,
        description = this.description,
        hosts = this.hosts,
        login = this.login,
        password = this.password,
        providerType = this.providerType
      )

    provider
  }
}

object ProviderApi {
  def fromProvider(provider: Provider): ProviderApi = {
    provider.providerType match {
      case ProviderLiterals.jdbcType =>
        val jdbcProviderMid = provider.asInstanceOf[JDBCProvider]

        new JDBCProviderApi(
          name = jdbcProviderMid.name,
          login = jdbcProviderMid.login,
          password = jdbcProviderMid.password,
          hosts = jdbcProviderMid.hosts,
          driver = jdbcProviderMid.driver,
          description = jdbcProviderMid.description
        )

      case _ =>
        new ProviderApi(
          name = provider.name,
          login = provider.login,
          password = provider.password,
          providerType = provider.providerType,
          hosts = provider.hosts,
          description = provider.description
        )
    }
  }
}