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

import com.bwsw.sj.common.si.model.provider.ESProvider
import com.bwsw.sj.common.utils.RestLiterals
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import scaldi.Injector


class ESProviderApi(name: String,
                    val login: String,
                    val password: String,
                    @JsonProperty("type") providerType: String,
                    hosts: Array[String],
                    description: Option[String] = Some(RestLiterals.defaultDescription),
                    creationDate: String)
  extends ProviderApi(name, providerType, hosts, description, creationDate) {

  @JsonIgnore
  override def to()(implicit injector: Injector): ESProvider = {
    new ESProvider(
      name = name,
      description = description.getOrElse(RestLiterals.defaultDescription),
      hosts = hosts,
      login = login,
      password = password,
      providerType = providerType,
      creationDate = creationDate)
  }
}
