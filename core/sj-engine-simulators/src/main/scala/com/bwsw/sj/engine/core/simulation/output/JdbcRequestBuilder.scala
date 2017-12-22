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
package com.bwsw.sj.engine.core.simulation.output

import java.sql.PreparedStatement

import com.bwsw.sj.common.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.jdbc.JdbcCommandBuilder
import com.bwsw.sj.engine.core.simulation.output.mock.jdbc.{JdbcClientMock, PreparedStatementMock}

/**
  * Provides method for building SQL query from [[com.bwsw.sj.common.engine.core.entities.OutputEnvelope]].
  *
  * @param outputEntity working entity gotten from
  *                     [[com.bwsw.sj.common.engine.core.output.OutputStreamingExecutor OutputStreamingExecutor]]
  * @param table        name of SQL-table
  * @author Pavel Tomskikh
  */
class JdbcRequestBuilder(outputEntity: Entity[(PreparedStatement, Int) => Unit],
                         table: String = JdbcRequestBuilder.defaultTable)
  extends OutputRequestBuilder[PreparedStatementMock] {

  private val client = new JdbcClientMock(table)
  override protected val commandBuilder: JdbcCommandBuilder =
    new JdbcCommandBuilder(client, transactionFieldName, outputEntity)

  /**
    * @inheritdoc
    */
  override def buildInsert(outputEnvelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[_]): PreparedStatementMock =
    commandBuilder.buildInsert(inputEnvelope.id, outputEnvelope.getFieldsValue).asInstanceOf[PreparedStatementMock]

  /**
    * @inheritdoc
    */
  override def buildDelete(inputEnvelope: TStreamEnvelope[_]): PreparedStatementMock =
    commandBuilder.buildDelete(inputEnvelope.id).asInstanceOf[PreparedStatementMock]
}

object JdbcRequestBuilder {
  val defaultTable = "entity"
}
