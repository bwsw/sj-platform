package com.bwsw.common.jdbc

import com.bwsw.sj.common.utils.JdbcLiterals._
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
   var txnField: String = _

   /**
     * This method return driver class name, related to driver name provided in service
     *
     * @return String: name of class of using driver
     */
   def driverClass: String = driver.toLowerCase match {
     case `postgresqlDriverName` => postgresqlDriver
     case `oracleDriverName` => oracleDriver
     case `mysqlDriverName` => mysqlDriver
     case _ => throw new RuntimeException(s"Existing drivers: $validDrivers")
   }

   /**
     * This method return prefix of server url: (prefix)://(host:port)/(database)
     *
     * @return String: prefix of server url
     */
   def driverPrefix: String = driver.toLowerCase match {
     case `postgresqlDriverName` => postgresqlDriverPrefix
     case `oracleDriverName` => oracleDriverPrefix
     case `mysqlDriverName` => mysqlDriverPrefix
     case _ => throw new RuntimeException(s"Existing drivers: $validDrivers")
   }

   def this(hosts:Array[String], driver:String, username:String, password:String, database:String, table:String, txnField:String) = {
     this
     this.hosts = hosts
     this.driver = driver
     this.username = username
     this.password = password
     this.database = database
     this.table = table
     this.txnField = txnField
   }
 }
