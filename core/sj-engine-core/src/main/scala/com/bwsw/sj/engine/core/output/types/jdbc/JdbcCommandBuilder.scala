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

import java.sql.PreparedStatement

import com.bwsw.common.jdbc.IJdbcClient
import com.bwsw.sj.common.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.CommandBuilder

/**
  * Provides methods for building jdbc [[java.sql.PreparedStatement]] to CRUD data
  *
  * @param transactionFieldName name of transaction field to check data on duplicate
  * @param entity               data
  * @author Ivan Kudryavtsev
  */
class JdbcCommandBuilder(client: IJdbcClient,
                         transactionFieldName: String,
                         entity: Entity[(PreparedStatement, Int) => Unit])
  extends CommandBuilder[PreparedStatement] {

  /**
    * Create a select prepared statement according to txn field
    */
  def select: PreparedStatement = {
    val sqlSelect = s"SELECT * FROM ${client.jdbcCCD.table.get} WHERE $transactionFieldName = ?"
    client.createPreparedStatement(sqlSelect)
  }

  /**
    * Create a remove prepared statement according to txn field
    */
  def delete: PreparedStatement = {
    val sqlRemove = s"DELETE FROM ${client.jdbcCCD.table.get} WHERE $transactionFieldName = ?"
    client.createPreparedStatement(sqlRemove)
  }

  /**
    * Create an insert prepared statement according to txn field and provided fields.
    */
  def insert: PreparedStatement = {
    val fields = entity.getFields.mkString(",") + "," + transactionFieldName
    val fieldsParams = List.fill(fields.split(",").length)("?").mkString(",")
    val sqlInsert = s"INSERT INTO ${client.jdbcCCD.table.get} ($fields) VALUES ($fieldsParams)"
    client.createPreparedStatement(sqlInsert)
  }

  /**
    * @inheritdoc
    */
  override def buildInsert(transaction: Long, fields: Map[String, Any]): PreparedStatement =
    buildInsert(transaction, fields, insert)

  /**
    * Places data in a placeholders in prepared statement
    *
    * @param transaction             transaction ID
    * @param fields                  data
    * @param insertPreparedStatement insertion prepared statement
    * @return insertion prepared statement
    */
  def buildInsert(transaction: Long,
                  fields: Map[String, Any],
                  insertPreparedStatement: PreparedStatement): PreparedStatement = {
    var position = 0
    entity.getFields.foreach { field =>
      val transformation = {
        if (fields.contains(field)) fields(field)
        else entity.getField(field).getDefaultValue
      }
      position += 1
      entity.getField(field).transform(transformation)(insertPreparedStatement, position)
    }

    position += 1
    insertPreparedStatement.setLong(position, transaction)

    insertPreparedStatement
  }

  /**
    * @inheritdoc
    */
  override def buildDelete(transaction: Long): PreparedStatement = {
    val deletePreparedStatement = delete
    deletePreparedStatement.setLong(1, transaction)

    deletePreparedStatement
  }

  def exists(transaction: Long): PreparedStatement = {
    val selectPreparedStatement = select
    selectPreparedStatement.setLong(1, transaction)

    selectPreparedStatement
  }
}