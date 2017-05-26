package com.bwsw.sj.engine.core.output.types.jdbc

import java.sql.PreparedStatement

import com.bwsw.common.jdbc.IJdbcClient
import com.bwsw.sj.engine.core.output.Entity

/**
  * Provides methods for building jdbc [[PreparedStatement]] to CRUD data
  *
  * @param transactionFieldName name of transaction field to check data on duplicate
  * @param entity               data
  * @author Ivan Kudryavtsev
  */

class JdbcCommandBuilder(client: IJdbcClient, transactionFieldName: String, entity: Entity[(PreparedStatement, Int) => Unit]) {
  /**
    * Create a select prepared statement according to txn field
    */
  def select: PreparedStatement = {
    val sqlSelect = s"SELECT * FROM ${client.jdbcCCD.table.get} WHERE $transactionFieldName = ?"
    client.createPreparedStatement(sqlSelect)
  }

  /**
    * Create a remove prepared statement according to txn field
    */
  def delete: PreparedStatement = {
    val sqlRemove = s"DELETE FROM ${client.jdbcCCD.table.get} WHERE $transactionFieldName = ?"
    client.createPreparedStatement(sqlRemove)
  }

  /**
    * Create an insert prepared statement according to txn field and provided fields.
    */
  def insert: PreparedStatement = {
    val fields = entity.getFields.mkString(",") + "," + transactionFieldName
    val fieldsParams = List.fill(fields.split(",").length)("?").mkString(",")
    val sqlInsert = s"INSERT INTO ${client.jdbcCCD.table.get} ($fields) VALUES ($fieldsParams)"
    client.createPreparedStatement(sqlInsert)
  }

  def buildInsert(transaction: Long, fields: Map[String, Any]): PreparedStatement = {
    val insertPreparedStatement = insert
    var t = 0
    val mv = entity.getFields.map(f => if (fields.contains(f)) {
      t+=1
      t -> entity.getField(f).transform(fields(f))
    }
    else {
      t+=1
      t -> entity.getField(f).transform(entity.getField(f).getDefaultValue)
    }
    )
    t+=1
    mv.foreach( {case (key: Int, value: ((PreparedStatement, Int) => Unit) ) => value.apply(insertPreparedStatement, key)})
    insertPreparedStatement.setLong(t, transaction)

    insertPreparedStatement
  }

  def buildDelete(transaction: Long): PreparedStatement = {
    val deletePreparedStatement = delete
    deletePreparedStatement.setLong(1, transaction)

    deletePreparedStatement
  }

  def exists(transaction: Long): PreparedStatement = {
    val selectPreparedStatement = select
    selectPreparedStatement.setLong(1, transaction)

    selectPreparedStatement
  }
}