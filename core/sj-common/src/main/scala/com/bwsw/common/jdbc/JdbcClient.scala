package com.bwsw.common.jdbc

import java.net.URLClassLoader
import java.sql.{Connection, Driver, PreparedStatement, SQLException}
import java.util.Properties

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.JdbcLiterals
import org.slf4j.LoggerFactory


// todo: Add multiple connection to databases.
/**
  * Class allows connecting to the specific database using a driver that is provided in the loaded jar file
  * that user have to upload to mongo and create three settings with 'jdbc' domain:
  * 1) driver.<driver_name> - name of file with JDBC driver (must exists in files) (e.g. "mysql-connector-java-5.1.6.jar")
  * 2) driver.<driver_name>.class - name of class of the driver (e.g. "com.mysql.jdbc.Driver")
  * 3) driver.<driver_name>.prefix - prefix of server url: (prefix)://(host:port)/(database), one of [jdbc:mysql, jdbc:postgresql, jdbc:oracle:thin]
  *
  * driver_name is used in JDBC provider ('driver' field)
  *
  * Also allows manipulating with elements of the specific table (only one table)
  *
  * @param jdbcCCD : connection data provider
  */
protected class JdbcClient(override val jdbcCCD: JdbcClientConnectionData) extends IJdbcClient {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val driver = createDriver()
  private val credential = createCredential()
  override var _connection: Option[Connection] = None

  private def createDriver(): Driver = {
    logger.info(s"Create a jdbc driver.")
    java.util.Locale.setDefault(java.util.Locale.ENGLISH)

    val driverFileName = jdbcCCD.driverFileName
    val jarFile = ConnectionRepository.getFileStorage.get(driverFileName, s"tmp/$driverFileName")
    val classLoader = new URLClassLoader(Array(jarFile.toURI.toURL), ClassLoader.getSystemClassLoader)
    val driver = classLoader.loadClass(jdbcCCD.driverClass).newInstance().asInstanceOf[Driver]

    driver
  }

  def start(): Unit = {
    val url = jdbcCCD.url
    logger.info(s"Create a connection to a jdbc database via url: $url.")
    java.util.Locale.setDefault(java.util.Locale.ENGLISH)

    _connection = Some(driver.connect(url, credential))
  }

  def checkConnectionToDatabase(): Boolean = {
    logger.info(s"Check a connection to a jdbc database (without specifying database name and table name).")
    java.util.Locale.setDefault(java.util.Locale.ENGLISH)
    val connection = driver.connect(jdbcCCD.urlWithoutDatabase, credential)

    val isDatabaseAvailable = connection.isValid(10)

    connection.close()

    isDatabaseAvailable
  }

  private def createCredential(): Properties = {
    val credential = new Properties()
    credential.setProperty("user", jdbcCCD.username)
    credential.setProperty("password", jdbcCCD.password)
    credential.setProperty("useSSL", "false")

    credential
  }
}

trait IJdbcClient {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected var _connection: Option[Connection]
  val jdbcCCD: JdbcClientConnectionData

  def isConnected: Boolean = _connection.isDefined && _connection.get.isValid(1)

  def tableExists(): Boolean = {
    _connection match {
      case Some(connection) =>
        logger.debug(s"Verify that the table '${jdbcCCD.table}' exists in a database.")
        jdbcCCD.driverPrefix match {
          case JdbcLiterals.oracleDriverPrefix =>
            try {
              val statement = connection.prepareStatement(s"SELECT COUNT(*) FROM ${jdbcCCD.table}")
              statement.execute()
              statement.close()
              true
            } catch {
              case _: SQLException => false
            }

          case _ =>
            var result: Boolean = false
            val dbResult = connection.getMetaData.getTables(null, null, jdbcCCD.table, null)
            while (dbResult.next) {
              if (dbResult.getString(3).nonEmpty) result = true
            }
            dbResult.close()

            result
        }
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

  def execute(sql: String): Int = {
    logger.debug(s"Try to execute a sql request: $sql.")
    _connection match {
      case Some(connection) =>
        val stmt = connection.createStatement()
        val result = try {
          stmt.executeUpdate(sql)
        } catch {
          case e: Exception =>
            logger.error(s"Sql request execution has failed: ${e.getMessage}.")
            throw new SQLException(e.getMessage)
        }
        stmt.close()
        result
      case None => throw new IllegalStateException("Jdbc client is not started. Start it first.")
    }
  }

  def close(): Unit = {
    logger.info(s"Close a connection to a jdbc database: '${jdbcCCD.database}'.")
    _connection match {
      case Some(connection) => connection.close()
      case None => throw new IllegalStateException("Jdbc client is not started. Start it first.")
    }
  }
}
