package com.bwsw.sj.engine.output.processing

import java.sql.PreparedStatement

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
                                       entity: Entity[_])
  extends OutputProcessor[T](outputStream, performanceMetrics) {

  private val jdbcStream = outputStream.asInstanceOf[JDBCSjStream]
  private val jdbcService = outputStream.service.asInstanceOf[JDBCService]
  private val jdbcClient = openConnection()
  private val jdbcCommandBuilder = new JdbcCommandBuilder(jdbcClient, transactionFieldName, entity.asInstanceOf[Entity[(PreparedStatement, Int) => Unit]])

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

  def delete(inputEnvelope: TStreamEnvelope[T]) = {
    logger.debug(s"Delete an envelope: '${inputEnvelope.id}' from JDBC.")

    val existPreparedStatement = jdbcCommandBuilder.exists(inputEnvelope.id)
    val resultSet = existPreparedStatement.executeQuery()
    val recordExists = resultSet.next()
    existPreparedStatement.close()
    if (recordExists) {
      val deletePreparedStatement = jdbcCommandBuilder.buildDelete(inputEnvelope.id)
      deletePreparedStatement.executeUpdate()
      deletePreparedStatement.close()
    }
  }

  def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]) = {
    logger.debug(s"Send an envelope: '${inputEnvelope.id}' to a JDBC stream: '${jdbcStream.name}'.")
    if (jdbcClient.tableExists()) {
      val preparedStatement = jdbcCommandBuilder.buildInsert(inputEnvelope.id, envelope.getFieldsValue)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    } else throw new RuntimeException(s"A table: '${jdbcStream.name}' doesn't exist so it is impossible to write data.")
  }

  override def close(): Unit = {
    logger.info(s"Close a JDBC connection at address: '${jdbcService.provider.hosts}'.")
    jdbcClient.close()
  }
}
