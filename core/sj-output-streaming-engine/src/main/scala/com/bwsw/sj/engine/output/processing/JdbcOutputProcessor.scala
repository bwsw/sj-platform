package com.bwsw.sj.engine.output.processing

import java.util.UUID

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.DAL.model.{JDBCService, JDBCSjStream, SjStream}
import com.bwsw.sj.engine.core.entities.{Envelope, JdbcEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.jdbc.JdbcCommandBuilder

class JdbcOutputProcessor[T <: AnyRef](outputStream: SjStream,
                                       performanceMetrics: OutputStreamingPerformanceMetrics)
  extends OutputProcessor[T](outputStream, performanceMetrics) {

  // TODO Here use jdbc command builder for create prepared statement

  private val jdbcStream = outputStream.asInstanceOf[JDBCSjStream]
  private val jdbcService = outputStream.service.asInstanceOf[JDBCService]
  private val jdbcClient = openConnection()
  private val jdbcCommandBuilder = new JdbcCommandBuilder(JdbcEnvelope.getTxnName, )

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
      setTxnField(JdbcEnvelope.getTxnName).
      build()
    client
  }

  def remove(envelope: TStreamEnvelope[T], entity: Entity[AnyRef]) = {

    logger.debug(s"Delete an envelope: '${envelope.id}' from JDBC.")
    val transaction = envelope.id //.toString.replaceAll("-", "")
    val lambda = jdbcCommandBuilder.buildDelete(transaction, )
    jdbcClient.removeByTransactionId(transaction)
  }

  def send(envelope: Envelope, inputEnvelope: TStreamEnvelope[T], entity: Entity[AnyRef]) = {
    val jdbcEnvelope = envelope.asInstanceOf[JdbcEnvelope]
    jdbcEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
    jdbcEnvelope.setV(jdbcStream.primary, UUID.randomUUID().toString)
    logger.debug(s"Send an envelope: '${jdbcEnvelope.txn}' to a JDBC stream: '${jdbcStream.name}'.")
    jdbcClient.write(jdbcEnvelope)
  }

  override def close(): Unit = {
    logger.info(s"Close a JDBC connection at address: '${jdbcService.provider.hosts}'.")
    jdbcClient.close()
  }
}
