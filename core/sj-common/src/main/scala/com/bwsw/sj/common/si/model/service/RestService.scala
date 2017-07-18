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
package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.utils.RestLiterals
import scaldi.Injector

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  * @author Pavel Tomskikh
  */
class RestService(name: String,
                  provider: String,
                  val basePath: String,
                  val httpScheme: String,
                  val httpVersion: String,
                  val headers: Map[String, String],
                  description: String,
                  serviceType: String)
                 (implicit injector: Injector)
  extends Service(serviceType, name, provider, description) {

  import messageResourceUtils.createMessage

  override def to(): RestServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository

    val modelService =
      new RestServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        basePath = this.basePath,
        httpScheme = RestLiterals.httpSchemeFromString(this.httpScheme),
        httpVersion = RestLiterals.httpVersionFromString(this.httpVersion),
        headers = this.headers.asJava
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val basePathAttributeName = "basePath"
    val httpVersionAttributeName = "httpVersion"
    val httpSchemeAttributeName = "httpScheme"
    val errors = new ArrayBuffer[String]()
    errors ++= super.validate()

    // 'provider' field
    errors ++= validateProvider()

    // 'basePath' field
    Option(basePath) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", basePathAttributeName)
      case Some(x) =>
        if (!x.startsWith("/"))
          errors += createMessage("entity.error.attribute.must", basePathAttributeName, "starts with '/'")
      case _ =>
    }

    // 'httpVersion' field
    Option(httpVersion) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", httpVersionAttributeName)
      case Some(x) =>
        if (!RestLiterals.httpVersions.contains(x))
          errors += createMessage(
            "entity.error.attribute.must.one_of",
            httpVersionAttributeName,
            RestLiterals.httpVersions.mkString("[", ", ", "]"))
    }

    // 'httpScheme' field
    Option(httpScheme) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", httpSchemeAttributeName)
      case Some(x) =>
        if (!RestLiterals.httpSchemes.contains(x))
          errors += createMessage(
            "entity.error.attribute.must.one_of",
            httpSchemeAttributeName,
            RestLiterals.httpSchemes.mkString("[", ", ", "]"))
    }

    errors
  }
}
