package com.bwsw.sj.engine.output.processing

import java.util.UUID

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.DAL.model.{JDBCService, JDBCSjStream, SjStream}
import com.bwsw.sj.engine.core.entities.{Envelope, JdbcEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics

class JdbcOutputProcessor(outputStream: SjStream,
                          performanceMetrics: OutputStreamingPerformanceMetrics)
  extends OutputProcessor(outputStream, performanceMetrics) {

  private val jdbcStream = outputStream.asInstanceOf[JDBCSjStream]
  private val jdbcService = outputStream.service.asInstanceOf[JDBCService]
  private val jdbcClient = openConnection()

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

  def remove(envelope: TStreamEnvelope) = {
    logger.debug(s"Delete an envelope: '${envelope.id}' from JDBC.")
    val transaction = envelope.id.toString.replaceAll("-", "")
    jdbcClient.removeByTransactionId(transaction)
  }

  def send(envelope: Envelope, inputEnvelope: TStreamEnvelope) = {
    val jdbcEnvelope = envelope.asInstanceOf[JdbcEnvelope]
    logger.debug(s"Send an envelope: '${jdbcEnvelope.txn}' to a JDBC stream: '${jdbcStream.name}'.")
    jdbcEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
    jdbcEnvelope.setV(jdbcStream.primary, UUID.randomUUID().toString)
    jdbcClient.write(jdbcEnvelope)
  }

  override def close(): Unit = {
    logger.info(s"Close a JDBC connection at address: '${jdbcService.provider.hosts}'.")
    jdbcClient.close()
  }
}
