package com.bwsw.common.jdbc

import java.sql.{Connection, DriverManager, SQLException}

import org.slf4j.LoggerFactory


// todo: Add multiple connection to databases.
/**
  * JDBC client - JDBC connection wrapper
  *
  * @param jdbcCCD : connection data provider
  */
protected class JdbcClient(var jdbcCCD: JdbcClientConnectionData) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var _connection: Option[Connection] = _
  createConnection()

  private def createConnection(): Unit = {
    val url = Array(jdbcCCD.driverPrefix, jdbcCCD.hosts.mkString("://", ",", "/"), jdbcCCD.database).mkString
    logger.info(s"Create a connection to a jdbc database via url: ${url.toString}.")
    java.util.Locale.setDefault(java.util.Locale.ENGLISH)
    Class.forName(jdbcCCD.driverClass)
    _connection = Some(DriverManager.getConnection(url, jdbcCCD.username, jdbcCCD.password))
  }

  def connection: Connection = _connection.get

  def connection_=(url: String, username: String, password: String): Unit = {
    _connection = Some(DriverManager.getConnection(url, username, password))
  }

  def isConnected: Boolean = _connection.isDefined

  def execute(sql: String) = {
    logger.debug(s"Try to execute a sql request: $sql.")
    val stmt = connection.createStatement()
    try stmt.executeUpdate(sql) catch {
      case e: Exception =>
        logger.error(s"Sql request execution has failed: ${e.getMessage}.")
        throw new SQLException(e.getMessage)
    }
  }

  def close() = {
    logger.info(s"Close a connection to a jdbc database: '${jdbcCCD.database}'.")
    connection.close()
  }

}




