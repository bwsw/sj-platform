package com.bwsw.common

/**
  * Created by diryavkin_dn on 10.10.16.
  */

import java.sql.{Connection, DriverManager, SQLException}


// todo: Add multiple connection to databases.
/**
  * JDBC client - JDBC connection wrapper
  *
  * @param jdbcCCD: connection data provider
  */
protected class JdbcClient (private var jdbcCCD: JdbcClientConnectionData) {
  private var _connection: Option[Connection] = _
  createConnection()

  private def createConnection(): Unit = {
    val url = Array(jdbcCCD.driverPrefix, jdbcCCD.hosts.mkString("://", ",", "/"), jdbcCCD.database).mkString
    java.util.Locale.setDefault(java.util.Locale.ENGLISH)
    _connection = Some(DriverManager.getConnection(url, jdbcCCD.username, jdbcCCD.password))
  }

  def connection: Connection = _connection.get

  def connection_= (url:String, username:String, password:String):Unit = {
    _connection = Some(DriverManager.getConnection(url, username, password))
  }

  def isConnected: Boolean = _connection.isDefined

  def write(data: Object) = {
    createDatatable(data)
    if (!checkTableExists()) {throw new RuntimeException("There is no table in database")}

    val attrs = getObjectAttributes(data)

    val columns = attrs.map(a => a._1).mkString(", ")
    val values = attrs.map(a => a._3.toString.mkString("'","","'")).mkString(", ")
    val sql = s"INSERT INTO ${jdbcCCD.table} ($columns) VALUES ($values);"

    val stmt = connection.createStatement()
    try stmt.executeUpdate(sql) catch {case e:Exception => throw new SQLException(e.getMessage)}

  }

  private def checkTableExists(): Boolean = {
    var result:Boolean = false
    val dbResult = connection.getMetaData.getTables(null, null, this.jdbcCCD.table, null)
    while (dbResult.next) {
      if (!dbResult.getString(3).isEmpty) result = true
    }
    result
  }

  /**
    * Method for catching attributes from object.
    * @param data: Some object
    * @return Array of (Attribute name, Type, Value)
    */
  private def getObjectAttributes(data: Object):Array[(java.lang.String, java.lang.Class[_], AnyRef)] = {
    data.getClass.getDeclaredMethods.filter {_.getReturnType != Void.TYPE}
      .map { method => (method.getName, method.getReturnType, method.invoke(data))}
  }

  private def createDatatable(data: Object): Unit = {
  }

}

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
  def driverClass: String = driver.toLowerCase match {
    case "postgresql" => "org.postgresql.Driver"
    case "oracle" => "oracle.jdbc.driver.OracleDriver"
    case "mysql" => "com.mysql.jdbc.Driver"
    case _ => throw new RuntimeException("Existing drivers: postgresql, mysql, oracle")
  }

  /**
    * This method return prefix of server url: (prefix)://(host:port)/(database)
    *
    * @return String: prefix of server url
    */
  def driverPrefix: String = driver.toLowerCase match {
    case "postgresql" => "jdbc:postgresql"
    case "oracle" => "jdbc:oracle:thin"
    case "mysql" => "jdbc:mysql"
    case _ => throw new RuntimeException("Existing drivers: postgresql, mysql, oracle")
  }

  def this(hosts:Array[String], driver:String, username:String, password:String, database:String, table:String) = {
    this
    this.hosts = hosts
    this.driver = driver
    this.username = username
    this.password = password
    this.database = database
    this.table = table
  }
}

/**
  * Builder class for JDBC client
  */
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
  def setTable(table: String) = {jdbcClientConnectionData.table=table; this}

  /**
    * Use this method if you have JDBC connection data provider
    *
    * @param jdbcClientConnectionData: JdbcClientConnectionData
    * @return this
    */
  def setJdbcClientConnectionData(jdbcClientConnectionData: JdbcClientConnectionData) = {
    this.jdbcClientConnectionData = jdbcClientConnectionData; this
  }
}

/**
  * JDBC client builder instance
  */
object JdbcClientBuilder extends JdbcClientBuilder {}



//
//
//// todo remove
//
//object a {
//  val age = 683242
//  val first = "LOLIK2"
//  val last = "LKILL"
//  val id = 10
//}
//
//object ap extends App {
////  Class.forName("org.postgresql.Driver")
////  val client = new JdbcClient(Set(("192", 5432)), "post", "root", "root")
////  val url = "jdbc:postgresql://0.0.0.0:5433/test/"
////  client.connection_= (url, "root1", "root1")
////  val conn = client.connection
//  val jdbcClient = JdbcClientBuilder.
//    setHosts(Array("0.0.0.0:5432")).
//    setDriver("postgresql").
//    setPassword("root").
//    setUsername("root").
//    setDatabase("test").
//    setTable("registration").
//    build()
//
////  println(jdbcClient.jdbcCCD.table)
//
//  jdbcClient.write(a)
//
////  val stmp = jdbcClient.connection.createStatement()
////  stmp.executeUpdate("CREATE TABLE REGISTRATION3 " +
////    "(id INTEGER not NULL, " +
////    " first VARCHAR(255), " +
////    " last VARCHAR(255), " +
////    " age INTEGER, " +
////    " PRIMARY KEY ( id ))")
//
//}
//////
//////
////////jdbc:postgresql://0.0.0.0:5432,0.0.0.0:5433/test
//////jdbc:postgresql://0.0.0.0:5432,0.0.0.0:5433/test