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

import java.sql.{Connection, Driver, PreparedStatement, SQLException}
import java.util.Properties

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{FileClassLoader, JdbcLiterals}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}
import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import scaldi.Injectable.inject
import scaldi.Injector

// todo: Add multiple connection to databases.
/**
  * Class allows connecting to the specific database using a driver that is provided in the loaded jar file
  * that user have to upload to mongo and create three settings with 'jdbc' domain:
  * 1) driver.<driver_name> - name of file with JDBC driver (must exists in files) (e.g. "mysql-connector-java-5.1.6.jar")
  * 2) driver.<driver_name>.class - name of class of the driver (e.g. "com.mysql.jdbc.Driver")
  * 3) driver.<driver_name>.prefix - prefix of server url: (prefix)://(host:port)/(database), one of [jdbc:mysql, jdbc:postgresql, jdbc:oracle:thin]
  *
  * driver_name is used in [[JDBCProviderDomain.driver]]
  *
  * Also allows manipulating with elements of the specific table (only one table)
  *
  * @param jdbcCCD connection data
  */

protected class JdbcClient(override val jdbcCCD: JdbcClientConnectionData)
                          (implicit injector: Injector) extends IJdbcClient {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val driver = createDriver()
  private val credential = createCredential()
  override var _connection: Option[Connection] = None

  private def createDriver(): Driver = {
    logger.info(s"Create a jdbc driver.")
    java.util.Locale.setDefault(java.util.Locale.ENGLISH)

    val driverFileName = jdbcCCD.driverFileName
    val jarFile = inject[ConnectionRepository].getFileStorage.get(driverFileName, s"tmp/$driverFileName")
    val classLoader = createClassLoader(jarFile.getName)
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
    credential.setProperty("connectTimeout", "500")

    credential
  }

  protected def createClassLoader(filename: String) = new FileClassLoader(inject[ConnectionRepository].getFileStorage, filename)
}

trait IJdbcClient {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected var _connection: Option[Connection]
  val jdbcCCD: JdbcClientConnectionData

  def isConnected: Boolean = _connection.isDefined && _connection.get.isValid(1)

  def tableExists(): Boolean = {
    _connection match {
      case Some(connection) =>
        jdbcCCD.table match {
          case Some(table) =>
            logger.debug(s"Verify that the table '$table' exists in a database.")
            jdbcCCD.driverPrefix match {
              case JdbcLiterals.oracleDriverPrefix =>
                Try {
                  val statement = connection.prepareStatement(s"SELECT COUNT(*) FROM $table")
                  statement.execute()
                  statement.close()
                }.isSuccess

              case _ =>
                var result: Boolean = false
                val dbResult = connection.getMetaData.getTables(null, null, table, null)
                while (dbResult.next) {
                  if (dbResult.getString(3).nonEmpty) result = true
                }
                dbResult.close()

                result
            }
          case None =>
            throw new IllegalStateException("Jdbc table not defined")
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
        val result = Try(stmt.executeUpdate(sql)) match {
          case Success(i) => i
          case Failure(e) =>
            logger.error(s"Sql request execution has failed: ${e.getMessage}.")
            throw new SQLException(e.getMessage)
        }
        stmt.close()
        result
      case None => throw new IllegalStateException("Jdbc client is not started. Start it first.")
    }
  }

  def close(): Unit = {
    logger.info(s"Close a connection to a jdbc database: '${jdbcCCD.database.get}'.")
    _connection match {
      case Some(connection) => connection.close()
      case None => throw new IllegalStateException("Jdbc client is not started. Start it first.")
    }
  }
}
