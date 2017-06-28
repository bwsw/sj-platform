package com.bwsw.sj.engine.core.simulation.output.mock.jdbc

import java.sql.{Blob, CallableStatement, Clob, Connection, DatabaseMetaData, NClob, PreparedStatement, SQLWarning, SQLXML, Savepoint, Statement, Struct}
import java.util.Properties
import java.util.concurrent.Executor
import java.{sql, util}

import com.bwsw.sj.engine.core.simulation.output.JdbcRequestBuilder

/**
  * Mock of [[Connection]] for [[JdbcRequestBuilder]]
  *
  * @author Pavel Tomskikh
  */
class JdbcConnectionMock extends Connection {
  override def isValid(timeout: Int): Boolean = true

  override def prepareStatement(sql: String): PreparedStatementMock = new PreparedStatementMock(sql)

  override def commit(): Unit = ???

  override def getHoldability: Int = ???

  override def setCatalog(catalog: String): Unit = ???

  override def setHoldability(holdability: Int): Unit = ???

  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int): PreparedStatement = ???

  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): PreparedStatement = ???

  override def prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement = ???

  override def prepareStatement(sql: String, columnIndexes: Array[Int]): PreparedStatement = ???

  override def prepareStatement(sql: String, columnNames: Array[String]): PreparedStatement = ???

  override def createClob(): Clob = ???

  override def setSchema(schema: String): Unit = ???

  override def setClientInfo(name: String, value: String): Unit = ???

  override def setClientInfo(properties: Properties): Unit = ???

  override def createSQLXML(): SQLXML = ???

  override def getCatalog: String = ???

  override def createBlob(): Blob = ???

  override def createStatement(): Statement = ???

  override def createStatement(resultSetType: Int, resultSetConcurrency: Int): Statement = ???

  override def createStatement(resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): Statement = ???

  override def abort(executor: Executor): Unit = ???

  override def setAutoCommit(autoCommit: Boolean): Unit = ???

  override def getMetaData: DatabaseMetaData = ???

  override def setReadOnly(readOnly: Boolean): Unit = ???

  override def prepareCall(sql: String): CallableStatement = ???

  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): CallableStatement = ???

  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): CallableStatement = ???

  override def setTransactionIsolation(level: Int): Unit = ???

  override def getWarnings: SQLWarning = ???

  override def releaseSavepoint(savepoint: Savepoint): Unit = ???

  override def nativeSQL(sql: String): String = ???

  override def isReadOnly: Boolean = ???

  override def createArrayOf(typeName: String, elements: Array[AnyRef]): sql.Array = ???

  override def setSavepoint(): Savepoint = ???

  override def setSavepoint(name: String): Savepoint = ???

  override def close(): Unit = ???

  override def createNClob(): NClob = ???

  override def rollback(): Unit = ???

  override def rollback(savepoint: Savepoint): Unit = ???

  override def setNetworkTimeout(executor: Executor, milliseconds: Int): Unit = ???

  override def setTypeMap(map: util.Map[String, Class[_]]): Unit = ???

  override def getAutoCommit: Boolean = ???

  override def clearWarnings(): Unit = ???

  override def getSchema: String = ???

  override def getNetworkTimeout: Int = ???

  override def isClosed: Boolean = ???

  override def getTransactionIsolation: Int = ???

  override def createStruct(typeName: String, attributes: Array[AnyRef]): Struct = ???

  override def getClientInfo(name: String): String = ???

  override def getClientInfo: Properties = ???

  override def getTypeMap: util.Map[String, Class[_]] = ???

  override def unwrap[T](iface: Class[T]): T = ???

  override def isWrapperFor(iface: Class[_]): Boolean = ???
}
