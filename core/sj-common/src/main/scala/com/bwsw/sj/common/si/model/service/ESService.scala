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

import java.util.Date

import com.bwsw.sj.common.dal.model.service.ESServiceDomain
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateNamespace
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class ESService(name: String,
                val index: String,
                provider: String,
                description: String,
                serviceType: String,
                creationDate: String)
               (implicit injector: Injector)
  extends Service(serviceType, name, provider, description, creationDate) {

  import messageResourceUtils.createMessage

  override def to(): ESServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository

    val modelService =
      new ESServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        index = this.index,
        creationDate = new Date()
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider()

    // 'index' field
    Option(this.index) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Index")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Index")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "index", x)
          }
        }
    }

    errors
  }
}
