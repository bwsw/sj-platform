package com.bwsw.common.jdbc

import java.net.URLClassLoader
import java.sql.{Connection, Driver, PreparedStatement, SQLException}
import java.util.Properties

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import org.slf4j.LoggerFactory


// todo: Add multiple connection to databases.
/**
  * JDBC client - JDBC connection wrapper
  *
  * @param jdbcCCD : connection data provider
  */
protected class JdbcClient(override val jdbcCCD: JdbcClientConnectionData) extends IJdbcClient {
  private val logger = LoggerFactory.getLogger(this.getClass)
  override var _connection: Option[Connection] = _
  createConnection()

  private def createConnection(): Unit = {
    val url = jdbcCCD.url
    logger.info(s"Create a connection to a jdbc database via url: $url.")
    java.util.Locale.setDefault(java.util.Locale.ENGLISH)

    val driverFileName = jdbcCCD.driverFileName
    val jarFile = ConnectionRepository.getFileStorage.get(driverFileName, s"tmp/$driverFileName")
    val classLoader = new URLClassLoader(Array(jarFile.toURI.toURL), ClassLoader.getSystemClassLoader)
    val driver = classLoader.loadClass(jdbcCCD.driverClass).newInstance().asInstanceOf[Driver]

    val credential = new Properties()
    credential.setProperty("user", jdbcCCD.username)
    credential.setProperty("password", jdbcCCD.password)
    _connection = Some(driver.connect(url, credential))
  }
}

trait IJdbcClient {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected var _connection: Option[Connection]
  val jdbcCCD: JdbcClientConnectionData

  def isConnected: Boolean = _connection.isDefined

  def tableExists(): Boolean = {
    _connection match {
      case Some(connection) =>
        logger.debug(s"Verify that the table '${jdbcCCD.table}' exists in a database.")
        var result: Boolean = false
        val dbResult = connection.getMetaData.getTables(null, null, jdbcCCD.table, null)
        while (dbResult.next) {
          if (dbResult.getString(3).nonEmpty) result = true
        }

        result
      case None => throw new IllegalStateException("Jdbc client is not started. Start it first.")
    }
  }

  def createPreparedStatement(sql: String): PreparedStatement = {
    _connection match {
      case Some(connection) =>
        connection.prepareStatement(sql)
      case None => throw new IllegalStateException("Jdbc client is not started. Start it first.")
    }
  }

  def execute(sql: String) = {
    logger.debug(s"Try to execute a sql request: $sql.")
    _connection match {
      case Some(connection) =>
        val stmt = connection.createStatement()
        try stmt.executeUpdate(sql) catch {
          case e: Exception =>
            logger.error(s"Sql request execution has failed: ${e.getMessage}.")
            throw new SQLException(e.getMessage)
        }
      case None => throw new IllegalStateException("Jdbc client is not started. Start it first.")
    }
  }

  def close() = {
    logger.info(s"Close a connection to a jdbc database: '${jdbcCCD.database}'.")
    _connection match {
      case Some(connection) => connection.close()
      case None => throw new IllegalStateException("Jdbc client is not started. Start it first.")
    }
  }
}
