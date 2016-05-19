package com.bwsw.sj.crud.rest.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Stream data case class
  */
case class ProviderData(name: String,
                        description: String,
                        login: String,
                        password: String,
                        @JsonProperty("type") providerType: String,
                        hosts: Array[String]
                       )