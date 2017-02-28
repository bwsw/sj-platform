package com.bwsw.common.jdbc

import java.sql.{Connection, DriverManager, SQLException}

import org.slf4j.LoggerFactory


// todo: Add multiple connection to databases.
/**
  * JDBC client - JDBC connection wrapper
  *
  * @param jdbcCCD : connection data provider
  */
protected class JdbcClient(private var jdbcCCD: JdbcClientConnectionData) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var _connection: Option[Connection] = _
  createConnection()

  private def createConnection(): Unit = {
    val url = Array(jdbcCCD.driverPrefix, jdbcCCD.hosts.mkString("://", ",", "/"), jdbcCCD.database).mkString
    logger.info(s"Create a connection to a jdbc database via url: ${url.toString}.")
    java.util.Locale.setDefault(java.util.Locale.ENGLISH)
    Class.forName(jdbcCCD.driverClass)
    _connection = Some(DriverManager.getConnection(url, jdbcCCD.username, jdbcCCD.password))
  }

  def connection: Connection = _connection.get

  def connection_=(url: String, username: String, password: String): Unit = {
    _connection = Some(DriverManager.getConnection(url, username, password))
  }

  def isConnected: Boolean = _connection.isDefined

  def write(data: Object) = {
    logger.debug(s"Write a data object to a database.")
    if (!checkTableExistence()) {
      logger.error(s"Writing has failed. There is no table in a database.")
      throw new RuntimeException("There is no table in database")
    }
    val sql = dataObjectToSqlRequest(data)
    execute(sql)
    logger.debug(s"Writing has finished.")
  }

  private def checkTableExistence(): Boolean = {
    logger.debug(s"Verify the existence of a table: '${jdbcCCD.table}' in a database.")
    var result: Boolean = false
    val dbResult = connection.getMetaData.getTables(null, null, jdbcCCD.table, null)
    while (dbResult.next) {
      if (!dbResult.getString(3).isEmpty) result = true
    }
    result
  }

  /**
    * Prepare object to sql with txn field.
    *
    * @param data
    * @return
    */
  private def dataObjectToSqlRequest(data: Object): String = {
    logger.debug(s"Convert a data object to a sql request.")
    var attrs = getObjectAttributes(data)
    attrs = attrs :+ (jdbcCCD.txnField, classOf[String], data.getClass.getMethods.find(_.getName == jdbcCCD.txnField).get.invoke(data))
    val columns = attrs.map(a => a._1).mkString(", ")
    val values = attrs.map(a => a._3.toString.mkString("'", "", "'")).mkString(", ")
    logger.debug(s"A conversion of a data object to a sql request has finished.")

    s"INSERT INTO `${jdbcCCD.table}` ($columns) VALUES ($values);"
  }


  /**
    * Method for catching attributes from object.
    *
    * @param data : Some object
    * @return Array of (Attribute name, Type, Value)
    */
  private def getObjectAttributes(data: Object): Array[(java.lang.String, java.lang.Class[_], Any)] = {
    logger.debug(s"Getting the data object attributes through a reflection.")
    data.getClass.getDeclaredMethods.filter {
      _.getReturnType != Void.TYPE
    }
      .map { method => (method.getName, method.getReturnType, method.invoke(data)) }
  }

  def removeByTransactionId(transactionId: String) = {
    def checkExistence(): Boolean = {
      val esql = s"SELECT * FROM `${jdbcCCD.table}` WHERE `${jdbcCCD.txnField}` = '$transactionId'"
      val stmt = connection.createStatement()
      val res = stmt.executeQuery(esql)
      res.next
    }

    if (checkExistence()) {
      logger.debug(s"Remove a record from the table: '${jdbcCCD.table}' by id: '$transactionId'.")
      val sql = s"DELETE FROM `${jdbcCCD.table}` WHERE `${jdbcCCD.txnField}` = '$transactionId'"
      execute(sql)
    }
  }

  def execute(sql: String) = {
    logger.debug(s"Try to execute a sql request: $sql.")
    val stmt = connection.createStatement()
    try stmt.executeUpdate(sql) catch {
      case e: Exception =>
        logger.error(s"Sql request execution has failed: ${e.getMessage}.")
        throw new SQLException(e.getMessage)
    }
  }

  def close() = {
    logger.info(s"Close a connection to a jdbc database: '${jdbcCCD.database}'.")
    connection.close()
  }

}




