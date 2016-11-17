package com.bwsw.common.jdbc

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
    Class.forName(jdbcCCD.driverClass)
    _connection = Some(DriverManager.getConnection(url, jdbcCCD.username, jdbcCCD.password))
  }

  def connection: Connection = _connection.get

  def connection_= (url:String, username:String, password:String):Unit = {
    _connection = Some(DriverManager.getConnection(url, username, password))
  }

  def isConnected: Boolean = _connection.isDefined

  def execute(sql: String) = {
    val stmt = connection.createStatement()
    try stmt.executeUpdate(sql) catch {case e:Exception => throw new SQLException(e.getMessage)}
  }

  /**
    * Prepare object to sql with txn field.
    * @param data
    * @return
    */
  private def prepareObjectToSQL(data: Object): String = {
    var attrs = getObjectAttributes(data)
    attrs = attrs:+(jdbcCCD.txnField, classOf[String], data.getClass.getMethods.find(_.getName == jdbcCCD.txnField).get.invoke(data))
    val columns = attrs.map(a => a._1).mkString(", ")
    val values = attrs.map(a => a._3.toString.mkString("'","","'")).mkString(", ")
    s"INSERT INTO ${jdbcCCD.table} ($columns) VALUES ($values);"
  }

  def write(data: Object) = {
    createTable(data)
    if (!checkTableExists()) {throw new RuntimeException("There is no table in database")}
    val sql = prepareObjectToSQL(data)
    execute(sql)
  }

  def removeByTransactionId(transactionId: String) = {
    val sql = s"DELETE FROM ${jdbcCCD.table} WHERE ${jdbcCCD.txnField} = '$transactionId'"
    def checkExists():Boolean = {
      val esql = s"SELECT * FROM ${jdbcCCD.table} WHERE ${jdbcCCD.txnField} = '$transactionId'"
      val stmt = connection.createStatement()
      val res = stmt.executeQuery(esql)
      res.next
    }
    if (checkExists())
      execute(sql)
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
  private def getObjectAttributes(data: Object):Array[(java.lang.String, java.lang.Class[_], Any)] = {
    data.getClass.getDeclaredMethods.filter {_.getReturnType != Void.TYPE}
      .map { method => (method.getName, method.getReturnType, method.invoke(data))}
  }

  private def createTable(data: Object): Unit = {
  }

  def close() = {
    connection.close()
  }

}




