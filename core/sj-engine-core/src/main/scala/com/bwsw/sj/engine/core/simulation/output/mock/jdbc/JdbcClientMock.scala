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
package com.bwsw.sj.engine.core.simulation.output.mock.jdbc

import java.sql.Connection

import com.bwsw.common.jdbc.{IJdbcClient, JdbcClientConnectionData}
import com.bwsw.sj.engine.core.simulation.output.JdbcRequestBuilder
import org.mockito.Mockito.{mock, when}

/**
  * Mock of [[IJdbcClient]] for for [[JdbcRequestBuilder]]
  *
  * @param table name of SQL-table
  * @author Pavel Tomskikh
  */
class JdbcClientMock(table: String) extends IJdbcClient {
  override protected var _connection: Option[Connection] = Some(new JdbcConnectionMock)
  override val jdbcCCD: JdbcClientConnectionData = mock(classOf[JdbcClientConnectionData])
  when(jdbcCCD.table).thenReturn(Some(table))
}
