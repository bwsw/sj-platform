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

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import com.bwsw.sj.common.dal.model.service.JDBCServiceDomain
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class JDBCService(name: String,
                  val database: String,
                  provider: String,
                  description: String,
                  serviceType: String)
                 (implicit injector: Injector)
  extends Service(serviceType, name, provider, description) {

  import messageResourceUtils.createMessage

  override def to(): JDBCServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository
    val provider = providerRepository.get(this.provider).get.asInstanceOf[JDBCProviderDomain]

    val modelService =
      new JDBCServiceDomain(
        name = this.name,
        description = this.description,
        provider = provider,
        database = this.database
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
      case None =>
        errors += createMessage("entity.error.attribute.required", "Database")
      case Some(dbName) =>
        if (dbName.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Database")
        } else if (errors.isEmpty) { //provider should exist in the following test
          val providerRepository = connectionRepository.getProviderRepository
          var database_exists: Boolean = false
          val provider = providerRepository.get(this.provider).get.asInstanceOf[JDBCProviderDomain]
          Try {
            val client = JdbcClientBuilder.
              setDriver(provider.driver).
              setDatabase(dbName).
              setHosts(provider.hosts).
              setUsername(provider.login).
              setPassword(provider.password).
              build()

            client.start()
            database_exists = true
            client.close()
          } match {
            case Success(_) =>
            case Failure(e: RuntimeException) =>
              errors += createMessage("error.cannot.create.client", e.getMessage)
            case Failure(e) =>
              e.printStackTrace()
          }

          if (!database_exists) {
            errors += createMessage("entity.error.doesnot.exist", "Database", dbName)
          }
        }
    }

    errors
  }
}
