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
package com.bwsw.sj.engine.core.simulation.mock.jdbc

import java.io.{InputStream, Reader}
import java.net.URL
import java.sql.{Blob, Clob, Connection, Date, NClob, ParameterMetaData, PreparedStatement, Ref, ResultSet, ResultSetMetaData, RowId, SQLWarning, SQLXML, Time, Timestamp}
import java.util.Calendar

import com.bwsw.sj.engine.core.simulation.JdbcRequestBuilder

/**
  * Mock of [[PreparedStatement]] for [[JdbcRequestBuilder]]
  *
  * @param sql prepared SQL-query
  * @author Pavel Tomskikh
  */
class PreparedStatementMock(sql: String) extends PreparedStatement {
  private val params = (sql.split('?') :+ "").map(SqlPart(_, ""))

  override def setBigDecimal(parameterIndex: Int, x: java.math.BigDecimal): Unit = set(parameterIndex, x)

  override def setInt(parameterIndex: Int, x: Int): Unit = set(parameterIndex, x)

  override def setLong(parameterIndex: Int, x: Long): Unit = set(parameterIndex, x)

  override def setFloat(parameterIndex: Int, x: Float): Unit = set(parameterIndex, x)

  override def setDouble(parameterIndex: Int, x: Double): Unit = set(parameterIndex, x)

  override def setByte(parameterIndex: Int, x: Byte): Unit = set(parameterIndex, x)

  override def setString(parameterIndex: Int, x: String): Unit = {
    params(parameterIndex).param = "'" + x.replaceAll("\\\\", "\\\\\\\\")
      .replaceAll("'", "\\\\'")
      .replaceAll("\"", "\\\\\"")
      .replaceAll("\n", "\\\\n") + "'"
  }

  override def setShort(parameterIndex: Int, x: Short): Unit = set(parameterIndex, x)

  override def setBoolean(parameterIndex: Int, x: Boolean): Unit =
    params(parameterIndex).param = x.toString.toUpperCase

  override def setDate(parameterIndex: Int, x: Date): Unit =
    params(parameterIndex).param = "'" + x.toString + "'"

  override def setBytes(parameterIndex: Int, x: Array[Byte]): Unit =
    params(parameterIndex).param = x.map(byte => Integer.toHexString(byte.toInt)).mkString("'", "", "'")

  def getQuery =
    params.foldLeft("")((query, queryParts) => query + queryParts.param + queryParts.part)

  private def set(parameterIndex: Int, x: Any) =
    params(parameterIndex).param = x.toString

  case class SqlPart(part: String, var param: String)


  override def setObject(parameterIndex: Int, x: scala.Any, targetSqlType: Int): Unit = ???

  override def setObject(parameterIndex: Int, x: scala.Any): Unit = ???

  override def setObject(parameterIndex: Int, x: scala.Any, targetSqlType: Int, scaleOrLength: Int): Unit = ???

  override def setNClob(parameterIndex: Int, value: NClob): Unit = ???

  override def setNClob(parameterIndex: Int, reader: Reader, length: Long): Unit = ???

  override def setNClob(parameterIndex: Int, reader: Reader): Unit = ???

  override def getParameterMetaData: ParameterMetaData = ???

  override def setTime(parameterIndex: Int, x: Time): Unit = ???

  override def setTime(parameterIndex: Int, x: Time, cal: Calendar): Unit = ???

  override def setUnicodeStream(parameterIndex: Int, x: InputStream, length: Int): Unit = ???

  override def addBatch(): Unit = ???

  override def execute(): Boolean = ???

  override def executeQuery(): ResultSet = ???

  override def setArray(parameterIndex: Int, x: java.sql.Array): Unit = ???

  override def setURL(parameterIndex: Int, x: URL): Unit = ???

  override def setAsciiStream(parameterIndex: Int, x: InputStream, length: Int): Unit = ???

  override def setAsciiStream(parameterIndex: Int, x: InputStream, length: Long): Unit = ???

  override def setAsciiStream(parameterIndex: Int, x: InputStream): Unit = ???

  override def setClob(parameterIndex: Int, x: Clob): Unit = ???

  override def setClob(parameterIndex: Int, reader: Reader, length: Long): Unit = ???

  override def setClob(parameterIndex: Int, reader: Reader): Unit = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream, length: Int): Unit = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream, length: Long): Unit = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream): Unit = ???

  override def setNString(parameterIndex: Int, value: String): Unit = ???

  override def getMetaData: ResultSetMetaData = ???

  override def executeUpdate(): Int = ???

  override def setNull(parameterIndex: Int, sqlType: Int): Unit = ???

  override def setNull(parameterIndex: Int, sqlType: Int, typeName: String): Unit = ???

  override def setSQLXML(parameterIndex: Int, xmlObject: SQLXML): Unit = ???

  override def setNCharacterStream(parameterIndex: Int, value: Reader, length: Long): Unit = ???

  override def setNCharacterStream(parameterIndex: Int, value: Reader): Unit = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader, length: Int): Unit = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader, length: Long): Unit = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader): Unit = ???

  override def setRef(parameterIndex: Int, x: Ref): Unit = ???

  override def setBlob(parameterIndex: Int, x: Blob): Unit = ???

  override def setBlob(parameterIndex: Int, inputStream: InputStream, length: Long): Unit = ???

  override def setBlob(parameterIndex: Int, inputStream: InputStream): Unit = ???

  override def setTimestamp(parameterIndex: Int, x: Timestamp): Unit = ???

  override def setTimestamp(parameterIndex: Int, x: Timestamp, cal: Calendar): Unit = ???

  override def setRowId(parameterIndex: Int, x: RowId): Unit = ???

  override def setDate(parameterIndex: Int, x: Date, cal: Calendar): Unit = ???

  override def clearParameters(): Unit = ???

  override def cancel(): Unit = ???

  override def getResultSetHoldability: Int = ???

  override def getMaxFieldSize: Int = ???

  override def getUpdateCount: Int = ???

  override def setPoolable(poolable: Boolean): Unit = ???

  override def getFetchSize: Int = ???

  override def setQueryTimeout(seconds: Int): Unit = ???

  override def setFetchDirection(direction: Int): Unit = ???

  override def setMaxRows(max: Int): Unit = ???

  override def setCursorName(name: String): Unit = ???

  override def getFetchDirection: Int = ???

  override def getResultSetType: Int = ???

  override def getMoreResults: Boolean = ???

  override def getMoreResults(current: Int): Boolean = ???

  override def addBatch(sql: String): Unit = ???

  override def execute(sql: String): Boolean = ???

  override def execute(sql: String, autoGeneratedKeys: Int): Boolean = ???

  override def execute(sql: String, columnIndexes: Array[Int]): Boolean = ???

  override def execute(sql: String, columnNames: Array[String]): Boolean = ???

  override def executeQuery(sql: String): ResultSet = ???

  override def isCloseOnCompletion: Boolean = ???

  override def getResultSet: ResultSet = ???

  override def getMaxRows: Int = ???

  override def setEscapeProcessing(enable: Boolean): Unit = ???

  override def executeUpdate(sql: String): Int = ???

  override def executeUpdate(sql: String, autoGeneratedKeys: Int): Int = ???

  override def executeUpdate(sql: String, columnIndexes: Array[Int]): Int = ???

  override def executeUpdate(sql: String, columnNames: Array[String]): Int = ???

  override def getQueryTimeout: Int = ???

  override def getWarnings: SQLWarning = ???

  override def getConnection: Connection = ???

  override def setMaxFieldSize(max: Int): Unit = ???

  override def isPoolable: Boolean = ???

  override def clearBatch(): Unit = ???

  override def close(): Unit = ???

  override def closeOnCompletion(): Unit = ???

  override def executeBatch(): Array[Int] = ???

  override def getGeneratedKeys: ResultSet = ???

  override def setFetchSize(rows: Int): Unit = ???

  override def clearWarnings(): Unit = ???

  override def getResultSetConcurrency: Int = ???

  override def isClosed: Boolean = ???

  override def unwrap[T](iface: Class[T]): T = ???

  override def isWrapperFor(iface: Class[_]): Boolean = ???
}
