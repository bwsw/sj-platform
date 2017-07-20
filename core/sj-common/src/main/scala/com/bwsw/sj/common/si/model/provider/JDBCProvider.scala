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
package com.bwsw.sj.common.si.model.provider

import java.util.Date

import com.bwsw.sj.common.config.{ConfigLiterals, NoSuchConfigException, SettingsUtils}
import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import com.bwsw.sj.common.utils.JdbcLiterals
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class JDBCProvider(name: String,
                   login: String,
                   password: String,
                   hosts: Array[String],
                   val driver: String,
                   description: String,
                   providerType: String,
                   creationDate: String)
                  (implicit injector: Injector)
  extends Provider(name, login, password, providerType, hosts, description, creationDate) {

  import messageResourceUtils.createMessage

  private val settingsUtils = inject[SettingsUtils]

  override def to(): JDBCProviderDomain = {
    val provider =
      new JDBCProviderDomain(
        name = this.name,
        description = this.description,
        hosts = this.hosts,
        login = this.login,
        password = this.password,
        driver = this.driver,
        creationDate = new Date()
      )

    provider
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = super.validate()

    // 'driver' field
    Option(this.driver) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Driver")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Driver")
        }
        else {
          Try(settingsUtils.getJdbcDriverFilename(x)) match {
            case Success(driverFileName) =>
              if (!connectionRepository.getFileStorage.exists(driverFileName))
                errors += createMessage("entity.error.file.required", driverFileName)
            case Failure(_: NoSuchConfigException) =>
              errors += createMessage("entity.error.config.required", ConfigLiterals.getDriverFilename(x))
            case Failure(e) => throw e
          }

          Try(settingsUtils.getJdbcDriverClass(x)) match {
            case Success(_) =>
            case Failure(_: NoSuchConfigException) =>
              errors += createMessage("entity.error.config.required", ConfigLiterals.getDriverClass(x))
            case Failure(e) => throw e
          }

          val prefixSettingName = ConfigLiterals.getDriverPrefix(x)
          Try(settingsUtils.getJdbcDriverPrefix(x)) match {
            case Success(prefix) =>
              if (!JdbcLiterals.validPrefixes.contains(prefix))
                errors += createMessage("entity.error.jdbc.incorrect.prefix", prefix, prefixSettingName)
            case Failure(_: NoSuchConfigException) =>
              errors += createMessage("entity.error.config.required", prefixSettingName)
            case Failure(e) => throw e
          }
        }
    }

    errors
  }
}
