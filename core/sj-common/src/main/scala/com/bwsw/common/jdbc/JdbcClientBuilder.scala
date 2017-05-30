package com.bwsw.common.jdbc

import com.bwsw.sj.common.SjModule
import org.slf4j.LoggerFactory

/**
  * Build [[JdbcClient]]. You can not create [[JdbcClient]] directly
  */

object JdbcClientBuilder {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private var hosts: Option[Array[String]] = None
  private var driver: Option[String] = None
  private var username: Option[String] = None
  private var password: Option[String] = None
  private var database: Option[String] = None
  private var table: Option[String] = None

  def buildCheck(): Unit = {
    driver match {
      case Some("") | None => throw new RuntimeException("Driver field must be declared.")
      case _ =>
    }
    database match {
      case Some("") | None => logger.warn("Database is not declared. It can lead to errors in the following.")
      case _ =>
    }
    table match {
      case Some("") | None => logger.warn("Table is not declared. It can lead to errors in the following.")
      case _ =>
    }
    username match {
      case Some("") | None => throw new RuntimeException("Username field must be declared.")
      case _ =>
    }
    password match {
      case Some("") | None => throw new RuntimeException("Password field must be declared.")
      case _ =>
    }
    hosts match {
      case None => throw new RuntimeException("Hosts field must be declared.")
      case _ =>
    }
  }

  def build(): JdbcClient = {
    buildCheck()
    val jdbcClientConnectionData = new JdbcClientConnectionData(
      hosts.get,
      driver.get,
      username.get,
      password.get,
      database,
      table)
    new JdbcClient(jdbcClientConnectionData)(SjModule.injector)
  }

  def setHosts(hosts: Array[String]): JdbcClientBuilder.type = {
    this.hosts = Option(hosts)
    this
  }

  def setDriver(driver: String): JdbcClientBuilder.type = {
    this.driver = Option(driver)
    this
  }

  def setUsername(username: String): JdbcClientBuilder.type = {
    this.username = Option(username)
    this
  }

  def setPassword(password: String): JdbcClientBuilder.type = {
    this.password = Option(password)
    this
  }

  def setDatabase(database: String): JdbcClientBuilder.type = {
    this.database = Option(database)
    this
  }

  def setTable(table: String): JdbcClientBuilder.type = {
    this.table = Option(table)
    this
  }

  def setJdbcClientConnectionData(jdbcClientConnectionData: JdbcClientConnectionData): JdbcClientBuilder.type = {
    hosts = Option(jdbcClientConnectionData.hosts)
    driver = Option(jdbcClientConnectionData.driver)
    username = Option(jdbcClientConnectionData.username)
    password = Option(jdbcClientConnectionData.password)
    database = jdbcClientConnectionData.database
    table = jdbcClientConnectionData.table
    this
  }
}
