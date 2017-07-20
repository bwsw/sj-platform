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
package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.RestService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

/**
  * @author Pavel Tomskikh
  */
class RestServiceApi(name: String,
                     provider: String,
                     val basePath: Option[String] = Some("/"),
                     val httpScheme: Option[String] = Some(RestLiterals.http),
                     val httpVersion: Option[String] = Some(RestLiterals.http_1_1),
                     val headers: Option[Map[String, String]] = Some(Map()),
                     description: Option[String] = Some(RestLiterals.defaultDescription),
                     @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.restType),
                     creationDate: String)
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.restType), name, provider, description, creationDate) {

  override def to()(implicit injector: Injector): RestService = {
    val modelService =
      new RestService(
        name = this.name,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        provider = this.provider,
        basePath = this.basePath.getOrElse("/"),
        httpScheme = this.httpScheme.getOrElse(RestLiterals.http),
        httpVersion = this.httpVersion.getOrElse(RestLiterals.http_1_1),
        headers = this.headers.getOrElse(Map()),
        serviceType = this.serviceType.getOrElse(ServiceLiterals.restType),
        creationDate = this.creationDate
      )

    modelService
  }
}
