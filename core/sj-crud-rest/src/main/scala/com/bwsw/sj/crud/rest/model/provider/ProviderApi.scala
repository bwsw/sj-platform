/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.crud.rest.model.provider

import com.bwsw.sj.common.si.model.provider.{JDBCProvider, Provider}
import com.bwsw.sj.common.utils.{ProviderLiterals, RestLiterals}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}
import scaldi.Injector

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[ProviderApi], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[JDBCProviderApi], name = ProviderLiterals.jdbcType)
))
class ProviderApi(val name: String,
                  val login: String,
                  val password: String,
                  @JsonProperty("type") val providerType: String,
                  val hosts: Array[String],
                  val description: Option[String] = Some(RestLiterals.defaultDescription)) {
  @JsonIgnore
  def to()(implicit injector: Injector): Provider = {
    val provider =
      new Provider(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        hosts = this.hosts,
        login = this.login,
        password = this.password,
        providerType = this.providerType
      )

    provider
  }
}

class ProviderApiCreator {
  def from(provider: Provider): ProviderApi = {
    provider.providerType match {
      case ProviderLiterals.jdbcType =>
        val jdbcProviderMid = provider.asInstanceOf[JDBCProvider]

        new JDBCProviderApi(
          name = jdbcProviderMid.name,
          login = jdbcProviderMid.login,
          password = jdbcProviderMid.password,
          hosts = jdbcProviderMid.hosts,
          driver = jdbcProviderMid.driver,
          description = Some(jdbcProviderMid.description)
        )

      case _ =>
        new ProviderApi(
          name = provider.name,
          login = provider.login,
          password = provider.password,
          providerType = provider.providerType,
          hosts = provider.hosts,
          description = Some(provider.description)
        )
    }
  }
}