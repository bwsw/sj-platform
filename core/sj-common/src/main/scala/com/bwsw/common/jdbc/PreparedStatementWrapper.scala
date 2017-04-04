package com.bwsw.common.jdbc

/**
  * Created by diryavkin_dn on 03.04.17.
  */

import java.sql.PreparedStatement

class PreparedStatementWrapper(val client: JdbcClient, val txnFieldName: String) {

  /**
    * Create select prepared statement according to txn field
    * @return
    */
  def select: PreparedStatement = {
    val sqlSelect = s"SELECT * FROM ${client.jdbcCCD.table} WHERE $txnFieldName = ?"
    client.connection.prepareStatement(sqlSelect)
  }

  /**
    * Create remove prepared statement according to txn field
    * @return
    */
  def remove: PreparedStatement = {
    val sqlRemove = s"DELETE FROM ${client.jdbcCCD.table} WHERE $txnFieldName = ?'"
    client.connection.prepareStatement(sqlRemove)
  }

  /**
    * Create insert prepared statement according to txn field and provided fields.
    * @param fields Comma separated fields name.
    * @return
    */
  def insert(fields: String): PreparedStatement = {
    val fieldsParams = List.fill(fields.split(",").length)("?").mkString(",")
    val sqlInsert = s"INSERT INTO ${client.jdbcCCD.table} ($fields) VALUES ($fieldsParams);"
    client.connection.prepareStatement(sqlInsert)
  }
}
