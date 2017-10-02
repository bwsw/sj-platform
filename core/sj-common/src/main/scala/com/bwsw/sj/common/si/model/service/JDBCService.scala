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

import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import com.bwsw.sj.common.dal.model.service.JDBCServiceDomain
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class JDBCService(name: String,
                  val database: String,
                  provider: String,
                  description: String,
                  serviceType: String,
                  creationDate: String)
                 (implicit injector: Injector)
  extends Service(serviceType, name, provider, description, creationDate) {

  import messageResourceUtils.createMessage

  override def to(): JDBCServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository
    val provider = providerRepository.get(this.provider).get.asInstanceOf[JDBCProviderDomain]

    val modelService =
      new JDBCServiceDomain(
        name = this.name,
        description = this.description,
        provider = provider,
        database = this.database,
        creationDate = new Date()
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider()

    // 'database' field
    Option(this.database) match {
      case Some(dbName) if dbName.nonEmpty =>
      case _ =>
        errors += createMessage("entity.error.attribute.required", "Database")
    }

    errors
  }
}
