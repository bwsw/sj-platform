package com.bwsw.sj.common.rest.entities.provider

import com.fasterxml.jackson.annotation.JsonProperty

case class ProviderData(name: String,
                        login: String,
                        password: String,
                        @JsonProperty("type") providerType: String,
                        hosts: Array[String],
                        description: String = "No description"
                       )