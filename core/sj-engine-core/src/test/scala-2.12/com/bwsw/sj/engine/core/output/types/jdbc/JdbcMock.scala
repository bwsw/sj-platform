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
package com.bwsw.sj.engine.core.output.types.jdbc

import java.sql.Connection

import com.bwsw.common.jdbc.{IJdbcClient, JdbcClientConnectionData}
import com.bwsw.sj.common.config.SettingsUtils
import com.mockrunner.jdbc.BasicJDBCTestCaseAdapter
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import scaldi.{Injector, Module}

class JdbcMock extends BasicJDBCTestCaseAdapter with IJdbcClient {
  private val settingsUtilsMock = new JdbcSettingsUtilsMock()

  override protected var _connection: Option[Connection] = Some(getJDBCMockObjectFactory.getMockConnection)
  override val jdbcCCD: JdbcClientConnectionData = new JdbcClientConnectionData(
    Array("localhost"),
    "mysql",
    "login",
    "password",
    Option("database"),
    Option("table")
  )(settingsUtilsMock.injector)
}

class JdbcSettingsUtilsMock extends MockitoSugar {
  private val settingsUtils = mock[SettingsUtils]
  private val driverName = "driver-name"

  when(settingsUtils.getJdbcDriverFilename(anyString())).thenReturn("mysql-connector-java-5.1.6.jar")
  when(settingsUtils.getJdbcDriverClass(anyString())).thenReturn("com.mysql.jdbc.Driver")
  when(settingsUtils.getJdbcDriverPrefix(anyString())).thenReturn("jdbc:mysql")

  private val module = new Module {
    bind[SettingsUtils] to settingsUtils
  }

  val injector: Injector = module.injector
}