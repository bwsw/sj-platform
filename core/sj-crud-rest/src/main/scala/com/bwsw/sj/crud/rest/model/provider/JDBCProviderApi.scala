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
                      @JsonProperty("type") providerType: Option[String] = Some(ProviderLiterals.jdbcType),
                      creationDate: String)
  extends ProviderApi(name, login, password, providerType.getOrElse(ProviderLiterals.jdbcType),
    hosts, description, creationDate) {

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
        providerType = this.providerType.getOrElse(ProviderLiterals.jdbcType),
        creationDate = this.creationDate
      )

    provider
  }
}
