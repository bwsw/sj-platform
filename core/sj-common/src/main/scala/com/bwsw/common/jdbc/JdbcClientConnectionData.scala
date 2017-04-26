package com.bwsw.common.jdbc

import com.bwsw.sj.common.config.ConfigurationSettingsUtils

/**
  * This class provide data for connection to database, required for initialize JDBC client object
  */
class JdbcClientConnectionData {
  var hosts: Array[String] = _
  var driver: String = _
  var username: String = _
  var password: String = _
  var database: String = _
  var table: String = _

  /**
    * This method return driver class name, related to driver name provided in service
    *
    * @return String: name of class of using driver
    */
  def driverClass: String = ConfigurationSettingsUtils.getJdbcDriverClass(driver)

  /**
    * This method return prefix of server url: (prefix)://(host:port)/(database)
    *
    * @return String: prefix of server url
    */
  def driverPrefix: String = ConfigurationSettingsUtils.getJdbcDriverPrefix(driver)

  /**
    * This method return name of file with jdbc driver
    *
    * @return String: name of file with jdbc driver
    */
  def driverFileName: String = ConfigurationSettingsUtils.getJdbcDriverFileName(driver)

  def this(hosts: Array[String], driver: String, username: String, password: String, database: String, table: String) = {
    this
    this.hosts = hosts
    this.driver = driver
    this.username = username
    this.password = password
    this.database = database
    this.table = table
  }
}
