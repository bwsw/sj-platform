/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.output.processing

import java.sql.PreparedStatement

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.dal.model.service.JDBCServiceDomain
import com.bwsw.sj.common.dal.model.stream.{JDBCStreamDomain, StreamDomain}
import com.bwsw.sj.common.engine.core.entities._
import com.bwsw.sj.common.engine.core.output.Entity
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.core.output.types.jdbc.{JdbcCommandBuilder, JdbcSender}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import scaldi.Injector

/**
  * ref. [[OutputProcessor]] object
  */
class JdbcOutputProcessor[T <: AnyRef](outputStream: StreamDomain,
                                       performanceMetrics: PerformanceMetrics,
                                       manager: OutputTaskManager,
                                       entity: Entity[_])
                                      (implicit injector: Injector)
  extends OutputProcessor[T](outputStream, performanceMetrics) {

  private val jdbcStream = outputStream.asInstanceOf[JDBCStreamDomain]
  private val jdbcService = outputStream.service.asInstanceOf[JDBCServiceDomain]
  private val jdbcClient = createClient()
  override protected val commandBuilder: JdbcCommandBuilder = new JdbcCommandBuilder(
    jdbcClient, transactionFieldName, entity.asInstanceOf[Entity[(PreparedStatement, Int) => Unit]])
  jdbcClient.start()

  private val jdbcSender = JdbcSender[T](commandBuilder, jdbcClient.supportsBatchUpdates)

  private def createClient() = {
    logger.info(s"Open a JDBC connection at address: '${jdbcService.provider.hosts.mkString(", ")}'.")
    val client = JdbcClientBuilder.
      setHosts(jdbcService.provider.hosts).
      setDriver(jdbcService.provider.driver).
      setUsername(jdbcService.provider.login).
      setPassword(jdbcService.provider.password).
      setTable(outputStream.name).
      setDatabase(jdbcService.database).
      build()

    client
  }

  def delete(inputEnvelope: TStreamEnvelope[T]): Unit = {
    logger.debug(s"Delete an envelope: '${inputEnvelope.id}' from JDBC.")

    val existPreparedStatement = commandBuilder.exists(inputEnvelope.id)
    val resultSet = existPreparedStatement.executeQuery()
    val recordExists = resultSet.next()
    existPreparedStatement.close()
    if (recordExists) {
      val deletePreparedStatement = commandBuilder.buildDelete(inputEnvelope.id)
      deletePreparedStatement.executeUpdate()
      deletePreparedStatement.close()
    }
  }

  def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit = {
    logger.debug(s"Send an envelope: '${inputEnvelope.id}' to a JDBC stream: '${jdbcStream.name}'.")
    jdbcSender.send(envelope, inputEnvelope)
  }

  override def close(): Unit = {
    logger.info(s"Close a JDBC connection at address: '${jdbcService.provider.hosts}'.")
    jdbcClient.close()
  }

  override def checkpoint(): Unit = jdbcSender.checkpoint()
}
