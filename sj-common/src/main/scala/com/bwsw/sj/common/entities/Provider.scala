package com.bwsw.sj.common.entities

import com.bwsw.common.DAL.Entity
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Entity for provider
  * Created: 22/04/2016
  *
  * @author Kseniya Tomskikh
  */
case class Provider(var name: String,
                    description: String,
                    hosts: List[String],
                    login: String,
                    password: String,
                    @JsonProperty("type") providerType: String) extends Entity