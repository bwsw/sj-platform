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
package com.bwsw.common.jdbc

import java.net.URI

import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.utils.JdbcLiterals._
import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * This class provide data for connection to database, required for initialize [[JdbcClient]]
  */

class JdbcClientConnectionData(val hosts: Array[String],
                               val driver: String,
                               val username: String,
                               val password: String,
                               val database: Option[String],
                               val table: Option[String])
                              (implicit val injector: Injector) {
  private val settingsUtils = inject[SettingsUtils]

  /**
    * This method returns a driver class name related to driver name provided in [[JDBCProviderDomain.driver]]
    *
    * @return String: name of class of using driver
    */
  def driverClass: String = settingsUtils.getJdbcDriverClass(driver)

  /**
    * This method returns a prefix of server url: (prefix)://(host:port)/(database)
    *
    * @return String: prefix of server url
    */
  def driverPrefix: String = settingsUtils.getJdbcDriverPrefix(driver)

  /**
    * This method returns a name of file with jdbc driver
    *
    * @return String: name of file with jdbc driver
    */
  def driverFileName: String = settingsUtils.getJdbcDriverFileName(driver)

  /**
    * This method returns a server URL
    */
  def url: String = database match {
    case Some(database_) => driverPrefix match {
      case `mysqlDriverPrefix` | `postgresqlDriverPrefix` =>
        s"$driverPrefix://${hosts.mkString(",")}/$database_"
      case `oracleDriverPrefix` =>
        var url = s"$driverPrefix:@(DESCRIPTION = (ADDRESS_LIST = "
        hosts.foreach { address =>
          val uri = new URI("dummy://" + address)
          url += s"(ADDRESS = (PROTOCOL = TCP) (HOST = ${uri.getHost}) (PORT = ${uri.getPort}))"
        }
        url += s")(CONNECT_DATA = (SERVICE_NAME = $database_)))"

        url
      case _ => throw new IllegalStateException(s"Incorrect JDBC prefix. Valid prefixes: $validPrefixes")
    }
    case None => throw new IllegalStateException("Database not defined")
  }

  /**
    * Server url without database name to check a connection
    */
  def urlWithoutDatabase: String = driverPrefix match {
    case `mysqlDriverPrefix` | `postgresqlDriverPrefix` =>
      s"$driverPrefix://${hosts.mkString(",")}"
    case `oracleDriverPrefix` =>
      var url = s"$driverPrefix:@(DESCRIPTION = (ADDRESS_LIST = "
      hosts.foreach { address =>
        val uri = new URI("dummy://" + address)
        url += s"(ADDRESS = (PROTOCOL = TCP) (HOST = ${uri.getHost}) (PORT = ${uri.getPort}))"
      }
      url += s"))"

      url
    case _ => throw new IllegalStateException(s"Incorrect JDBC prefix. Valid prefixes: $validPrefixes")
  }
}
