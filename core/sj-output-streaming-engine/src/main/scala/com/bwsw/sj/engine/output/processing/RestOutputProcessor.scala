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

import com.bwsw.common.JsonSerializer
import com.bwsw.common.rest.RestClient
import com.bwsw.sj.common.dal.model.stream.RestStreamDomain
import com.bwsw.sj.common.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.rest.RestCommandBuilder
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics

import scala.collection.JavaConverters._

/**
  * This class used in [[com.bwsw.sj.engine.output.task.OutputTaskEngine]] for sending data to a RESTful storage
  *
  * @param restOutputStream   stream indicating the specific RESTful storage
  * @param performanceMetrics set of metrics that characterize performance of an output streaming module
  * @author Pavel Tomskikh
  */
class RestOutputProcessor[T <: AnyRef](restOutputStream: RestStreamDomain,
                                       performanceMetrics: OutputStreamingPerformanceMetrics,
                                       manager: OutputTaskManager,
                                       entity: Entity[_])
  extends OutputProcessor[T](restOutputStream, performanceMetrics) {

  private val jsonSerializer = new JsonSerializer(ignoreUnknown = true)
  override protected val commandBuilder: RestCommandBuilder = new RestCommandBuilder(
    transactionFieldName = transactionFieldName,
    jsonSerializer = jsonSerializer)

  private val service = restOutputStream.service
  private val client = new RestClient(
    service.provider.hosts.toSet,
    service.basePath + "/" + restOutputStream.name,
    service.httpVersion,
    Map(service.headers.asScala.toList: _*),
    Option(service.provider.name),
    Option(service.provider.password)
  )

  override def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit = {
    logger.debug(createLogMessage("Write an output envelope to RESTful stream."))

    val posted = client.execute(commandBuilder.buildInsert(inputEnvelope.id, envelope.getFieldsValue))
    if (!posted) {
      val errorMessage = createLogMessage(s"Cannot send envelope '${inputEnvelope.id}'.")
      logger.error(errorMessage)
      delete(inputEnvelope)
      throw new RuntimeException(errorMessage)
    }
  }

  override def delete(envelope: TStreamEnvelope[T]): Unit = {
    logger.debug(createLogMessage(s"Delete a transaction: '${envelope.id}' from RESTful stream."))
    val deleted = client.execute(commandBuilder.buildDelete(envelope.id))
    if (!deleted)
      logger.warn(createLogMessage(s"Transaction '${envelope.id}' not deleted."))
  }

  override def close(): Unit = client.close()

  private def createLogMessage(message: String) = s"Task: '${manager.taskName}'. $message"
}
