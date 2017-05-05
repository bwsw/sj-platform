package com.bwsw.common.jdbc

import org.slf4j.LoggerFactory

/**
  * Builder class for JDBC client
  */
object JdbcClientBuilder {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var jdbcClientConnectionData = new JdbcClientConnectionData()

  def buildCheck(): Unit = {
    jdbcClientConnectionData.driver match {
      case "" | null => throw new RuntimeException("Driver field must be declared.")
      case _: String =>
    }
    jdbcClientConnectionData.database match {
      case "" | null => logger.warn("Database is not declared. It can lead to errors in the following.")
      case _ =>
    }
    jdbcClientConnectionData.table match {
      case "" | null => logger.warn("Table is not declared. It can lead to errors in the following.")
      case _ =>
    }
    jdbcClientConnectionData.username match {
      case "" | null => throw new RuntimeException("Username field must be declared.")
      case _: String =>
    }
    jdbcClientConnectionData.password match {
      case "" | null => throw new RuntimeException("Password field must be declared.")
      case _: String =>
    }
    jdbcClientConnectionData.hosts match {
      case null => throw new RuntimeException("Hosts field must be declared.")
      case _ =>
    }
  }

  def build(): JdbcClient = {
    buildCheck()
    new JdbcClient(jdbcClientConnectionData)
  }

  def setHosts(hosts: Array[String]): JdbcClientBuilder.type = {
    jdbcClientConnectionData.hosts = hosts
    this
  }

  def setDriver(driver: String): JdbcClientBuilder.type = {
    jdbcClientConnectionData.driver = driver
    this
  }

  def setUsername(username: String): JdbcClientBuilder.type = {
    jdbcClientConnectionData.username = username
    this
  }

  def setPassword(password: String): JdbcClientBuilder.type = {
    jdbcClientConnectionData.password = password
    this
  }

  def setDatabase(database: String): JdbcClientBuilder.type = {
    jdbcClientConnectionData.database = database
    this
  }

  def setTable(table: String): JdbcClientBuilder.type = {
    jdbcClientConnectionData.table = table
    this
  }

  /**
    * Use this method if you have JDBC connection data provider
    *
    * @param jdbcClientConnectionData : JdbcClientConnectionData
    * @return this
    */
  def setJdbcClientConnectionData(jdbcClientConnectionData: JdbcClientConnectionData): JdbcClientBuilder.type = {
    this.jdbcClientConnectionData = jdbcClientConnectionData
    this
  }
}
