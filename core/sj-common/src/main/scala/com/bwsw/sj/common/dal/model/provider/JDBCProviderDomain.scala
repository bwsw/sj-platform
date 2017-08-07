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
package com.bwsw.sj.common.dal.model.provider

import java.sql.SQLException
import java.util.Date

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.SjModule
import com.bwsw.sj.common.utils.ProviderLiterals

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class JDBCProviderDomain(override val name: String,
                         override val description: String,
                         override val hosts: Array[String],
                         override val login: String,
                         override val password: String,
                         val driver: String,
                         override val creationDate: Date)
  extends ProviderDomain(name, description, hosts, login, password, ProviderLiterals.jdbcType, creationDate) {

  override def checkJdbcConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    Try {
      val client = JdbcClientBuilder.
        setHosts(hosts).
        setDriver(driver).
        setUsername(login).
        setPassword(password).
        build()(SjModule.injector)

      client.checkConnectionToDatabase()
    } match {
      case Success(_) =>
      case Failure(ex: SQLException) =>
        ex.printStackTrace()
        errors += s"Cannot gain an access to JDBC on '$address'"
      case Failure(e) => throw e
    }

    errors
  }
}

