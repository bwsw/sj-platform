package com.bwsw.sj.engine.output.processing

import java.sql.PreparedStatement

import com.bwsw.common.jdbc.{JdbcClientBuilder, PreparedStatementWrapper}
import com.bwsw.sj.common.DAL.model.{JDBCService, JDBCSjStream, SjStream}
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.jdbc.JdbcCommandBuilder
import com.bwsw.sj.engine.output.task.OutputTaskManager

class JdbcOutputProcessor[T <: AnyRef](outputStream: SjStream,
                                       performanceMetrics: OutputStreamingPerformanceMetrics,
                                       manager: OutputTaskManager,
                                       entity: Entity[_])
  extends OutputProcessor[T](outputStream, performanceMetrics) {

  private val jdbcStream = outputStream.asInstanceOf[JDBCSjStream]
  private val jdbcService = outputStream.service.asInstanceOf[JDBCService]
  private val jdbcClient = openConnection()
  private val jdbcCommandBuilder = new JdbcCommandBuilder(txnFieldName, entity.asInstanceOf[Entity[(PreparedStatement, Int) => Unit]])
  private var preparedStatement: PreparedStatement = null
  private val preparedStatementWrapper = new PreparedStatementWrapper(jdbcClient, txnFieldName)

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


    preparedStatement = jdbcCommandBuilder.exists(inputEnvelope.id, preparedStatementWrapper.select)
    val res = preparedStatement.executeQuery()
    val recordExists = res.next()
    if (recordExists) {
      preparedStatement = jdbcCommandBuilder.buildDelete(inputEnvelope.id, preparedStatementWrapper.remove)
      preparedStatement.executeUpdate()
    }
  }

  private def tableExists(): Boolean = {
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
    if (tableExists()) {
      val fields = entity.asInstanceOf[Entity[(PreparedStatement, Int) => Unit]].getFields.mkString(",") + "," + txnFieldName
      val jdbcFieldsValue = envelope.getFieldsValue
      preparedStatement = jdbcCommandBuilder.buildInsert(inputEnvelope.id, jdbcFieldsValue, preparedStatementWrapper.insert(fields))
      preparedStatement.executeUpdate()
    }

  }

  override def close(): Unit = {
    logger.info(s"Close a JDBC connection at address: '${jdbcService.provider.hosts}'.")
    jdbcClient.close()
  }
}
