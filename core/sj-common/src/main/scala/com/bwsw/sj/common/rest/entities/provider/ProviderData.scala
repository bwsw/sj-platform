package com.bwsw.sj.common.rest.entities.provider

import com.bwsw.sj.common.DAL.model.Provider
import com.fasterxml.jackson.annotation.JsonProperty

case class ProviderData(name: String,
                        login: String,
                        password: String,
                        @JsonProperty("type") providerType: String,
                        hosts: Array[String],
                        description: String = "No description"
                       ) {
  def asProvider() = {
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
