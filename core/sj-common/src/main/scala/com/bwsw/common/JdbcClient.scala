package com.bwsw.common

/**
  * Created by diryavkin_dn on 10.10.16.
  */

import java.sql.{Connection, DriverManager}



protected class JdbcClient (jdbcCCD: JdbcClientConnectionData) {

  val url = Array(jdbcCCD.driverPrefix, jdbcCCD.hosts.mkString("://", ",", "/"), jdbcCCD.database).mkString

  java.util.Locale.setDefault(java.util.Locale.ENGLISH)
  Class.forName(jdbcCCD.driverClass)
  private var _connection = DriverManager.getConnection(url, jdbcCCD.username, jdbcCCD.password)

  def connection: Connection = _connection

  def connection_= (url:String, username:String, password:String):Unit = {
    _connection = DriverManager.getConnection(url, username, password)
  }
}

class JdbcClientConnectionData {
  var hosts: Array[String] = _
  var driver: String = _
  var username: String = _
  var password: String = _
  var database: String = _

  def driverClass: String = driver.toLowerCase match {
    case "postgresql" => "org.postgresql.Driver"
    case "oracle" => "oracle.jdbc.driver.OracleDriver"
    case "mysql" => "com.mysql.jdbc.Driver"
    case _ => throw new RuntimeException("Existing drivers: postgresql, mysql, oracle")
  }

  def driverPrefix: String = driver.toLowerCase match {
    case "postgresql" => "jdbc:postgresql"
    case "oracle" => "jdbc:oracle:thin"
    case "mysql" => "jdbc:mysql"
    case _ => throw new RuntimeException("Existing drivers: postgresql, mysql, oracle")
  }

  def this(hosts:Array[String], driver:String, username:String, password:String, database:String) = {
    this
    this.hosts = hosts
    this.driver = driver
    this.username = username
    this.password = password
    this.database = database
  }
}


protected class JdbcClientBuilder{
  private var jdbcClientConnectionData = new JdbcClientConnectionData()

  def buildCheck() = {
    jdbcClientConnectionData.database match {
      case ""|null => throw new RuntimeException("Database name must be declared.")
      case _:String =>
    }
  }

  def build(): JdbcClient = {
    buildCheck()
    new JdbcClient(jdbcClientConnectionData)
  }

  def setHosts(hosts: Array[String]) = {jdbcClientConnectionData.hosts=hosts; this}
  def setDriver(driver: String) = {jdbcClientConnectionData.driver=driver; this}
  def setUsername(username: String) = {jdbcClientConnectionData.username=username; this}
  def setPassword(password: String) = {jdbcClientConnectionData.password=password; this}
  def setDatabase(database: String) = {jdbcClientConnectionData.database=database; this}

  def setJdbcClientConnectionData(jdbcClientConnectionData: JdbcClientConnectionData) = {
    this.jdbcClientConnectionData = jdbcClientConnectionData; this
  }
}

object JdbcClientBuilder extends JdbcClientBuilder {}

// todo remove
//object ap extends App {
////  Class.forName("org.postgresql.Driver")
////  val client = new JdbcClient(Set(("192", 5432)), "post", "root", "root")
////  val url = "jdbc:postgresql://0.0.0.0:5433/test/"
////  client.connection_= (url, "root1", "root1")
////  val conn = client.connection
//  val jdbcClient = JdbcClientBuilder.
//    setHosts(Array("0.0.0.0:5433")).
//    setDriver("postgresql").
//    setPassword("root").
//    setUsername("root").
//    setDatabase("test123").
//    build()
//
//  val stmp = jdbcClient.connection.createStatement()
//  stmp.executeUpdate("CREATE TABLE REGISTRATION3 " +
//    "(id INTEGER not NULL, " +
//    " first VARCHAR(255), " +
//    " last VARCHAR(255), " +
//    " age INTEGER, " +
//    " PRIMARY KEY ( id ))")
//}
//
//
////jdbc:postgresql://0.0.0.0:5432,0.0.0.0:5433/test
////jdbc:postgresql://0.0.0.0:5432,0.0.0.0:5433/test