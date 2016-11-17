package com.bwsw.sj.engine.output.task.engine.Handler

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.engine.core.entities.{Envelope, JdbcEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.sj.common.DAL.model.{JDBCService, JDBCSjStream, SjStream}
import java.util.{Calendar, UUID}

/**
  * Created by diryavkin_dn on 07.11.16.
  */
class JdbcOutputHandler(outputStream: SjStream,
                        performanceMetrics: OutputStreamingPerformanceMetrics,
                        manager: OutputTaskManager)
                        extends OutputHandler(outputStream, performanceMetrics, manager) {

  val jdbcService = outputStream.service.asInstanceOf[JDBCService]
  val jdbcClient = openConnection()

  def openConnection() = {
    logger.info(s"Task: ${manager.taskName}. Open output JDBC connection.\n")
    val hosts = jdbcService.provider.hosts

    val client = JdbcClientBuilder.
      setHosts(hosts).
      setDriver(jdbcService.driver).
      setUsername(jdbcService.provider.login).
      setPassword(jdbcService.provider.password).
      setTable(outputStream.name).
      setDatabase(jdbcService.database).
      setTxnField(new JdbcEnvelope().getTxnName).
      build()
    client
  }

  def prepare(inputEnvelope: TStreamEnvelope, wasFirstCheckpoint: Boolean) = {
    remove(inputEnvelope, wasFirstCheckpoint)
  }

  def remove(envelope: TStreamEnvelope, wasFirstCheckpoint: Boolean) = {
    if (!wasFirstCheckpoint) {
      val transaction = envelope.id.toString.replaceAll("-", "")
      jdbcClient.removeByTransactionId(transaction)
    }
  }

  def send(envelope: Envelope, inputEnvelope: TStreamEnvelope) = {
    val jdbcEnvelope = envelope.asInstanceOf[JdbcEnvelope]
    val jdbcStream = outputStream.asInstanceOf[JDBCSjStream]
    jdbcEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
    jdbcEnvelope.setV(jdbcStream.primary, UUID.randomUUID().toString)
    jdbcClient.write(jdbcEnvelope)
  }
}
