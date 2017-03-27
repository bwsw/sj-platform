package com.bwsw.sj.engine.output.processing

import java.sql.PreparedStatement
import java.util.UUID

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.DAL.model.{JDBCService, JDBCSjStream, SjStream}
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.jdbc.JdbcCommandBuilder
import com.bwsw.sj.engine.output.task.OutputTaskManager

class JdbcOutputProcessor[T <: AnyRef](outputStream: SjStream,
                                       performanceMetrics: OutputStreamingPerformanceMetrics,
                                       manager: OutputTaskManager,
                                       entity: AnyRef)
  extends OutputProcessor[T](outputStream, performanceMetrics) {

  // TODO Here use jdbc command builder for create prepared statement

  private val jdbcStream = outputStream.asInstanceOf[JDBCSjStream]
  private val jdbcService = outputStream.service.asInstanceOf[JDBCService]
  private val jdbcClient = openConnection()
  private val jdbcCommandBuilder = new JdbcCommandBuilder("txn", entity.asInstanceOf[Entity[(PreparedStatement, Int) => Unit]])

  private def openConnection() = {
    logger.info(s"Open a JDBC connection at address: '${jdbcService.provider.hosts}'.")
    val hosts = jdbcService.provider.hosts

    val client = JdbcClientBuilder.
      setHosts(hosts).
      setDriver(jdbcService.driver).
      setUsername(jdbcService.provider.login).
      setPassword(jdbcService.provider.password).
      setTable(outputStream.name).
      setDatabase(jdbcService.database).
      build()
    client
  }

  def remove(inputEnvelope: TStreamEnvelope[T]) = {
    logger.debug(s"Delete an envelope: '${inputEnvelope.id}' from JDBC.")
    val sqlRemove = s"DELETE FROM `${jdbcClient.jdbcCCD.table}` WHERE `${jdbcCommandBuilder.getTransactionFieldName}` = ?'"
    val removePreparedStatement = jdbcClient.connection.prepareStatement(sqlRemove)
    val sqlSelect = s"SELECT * FROM `${jdbcClient.jdbcCCD.table}` WHERE `${jdbcCommandBuilder.getTransactionFieldName}` = ?"
    val selectPreparedStatement = jdbcClient.connection.prepareStatement(sqlSelect)
    if (jdbcCommandBuilder.exists(inputEnvelope.id, selectPreparedStatement)) {
      val preparedStatement = jdbcCommandBuilder.buildDelete(inputEnvelope.id, removePreparedStatement)
      preparedStatement.executeUpdate()
    }
  }

  def tableExists(): Boolean = {
    logger.debug(s"Verify the table '${jdbcClient.jdbcCCD.table}' exists in a database.")
    var result: Boolean = false
    val dbResult = jdbcClient.connection.getMetaData.getTables(null, null, jdbcClient.jdbcCCD.table, null)
    while (dbResult.next) {
      if (!dbResult.getString(3).isEmpty) result = true
    }
    result
  }

  def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]) = {
    logger.debug(s"Send an envelope: '${inputEnvelope.id}' to a JDBC stream: '${jdbcStream.name}'.")
    val fields = entity.asInstanceOf[Entity[(PreparedStatement, Int) => Unit]].getFields.mkString(",") + "," + jdbcCommandBuilder.getTransactionFieldName
    val fieldsParams = List.fill(fields.split(",").length)("?").mkString(",")
    val sqlInsert = s"INSERT INTO `${jdbcClient.jdbcCCD.table}` ($fields) VALUES ($fieldsParams);"
    val preparedStatement = jdbcClient.connection.prepareStatement(sqlInsert)
    val jdbcFieldsValue = envelope.getFieldsValue
    val readyStatement = jdbcCommandBuilder.buildInsert(inputEnvelope.id, jdbcFieldsValue, preparedStatement)
    if (tableExists()) readyStatement.executeUpdate()
  }

  override def close(): Unit = {
    logger.info(s"Close a JDBC connection at address: '${jdbcService.provider.hosts}'.")
    jdbcClient.close()
  }
}
