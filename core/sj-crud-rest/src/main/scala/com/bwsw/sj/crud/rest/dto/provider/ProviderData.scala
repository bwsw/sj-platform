package com.bwsw.sj.crud.rest.dto.provider

import com.bwsw.sj.common._dal.model.provider.{JDBCProvider, Provider}
import com.bwsw.sj.common.utils.ProviderLiterals
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[ProviderData], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[JDBCProviderData], name = ProviderLiterals.jdbcType)
))
class ProviderData(val name: String,
                   val login: String,
                   val password: String,
                   @JsonProperty("type") val providerType: String,
                   val hosts: Array[String],
                   val description: String = "No description"
                  ) {
  @JsonIgnore
  def asModelProvider(): Provider = {
    val provider = new Provider(
      this.name,
      this.description,
      this.hosts,
      this.login,
      this.password,
      this.providerType
    )

    provider
  }
}

object ProviderData {
  def fromModelProvider(provider: Provider): ProviderData = {
    provider.providerType match {
      case ProviderLiterals.jdbcType =>
        val jDBCProvider = provider.asInstanceOf[JDBCProvider]

        new JDBCProviderData(
          jDBCProvider.name,
          jDBCProvider.login,
          jDBCProvider.password,
          jDBCProvider.providerType,
          jDBCProvider.hosts,
          jDBCProvider.driver,
          jDBCProvider.description
        )
      case _ =>
        new ProviderData(
          provider.name,
          provider.login,
          provider.password,
          provider.providerType,
          provider.hosts,
          provider.description
        )
    }
  }
}