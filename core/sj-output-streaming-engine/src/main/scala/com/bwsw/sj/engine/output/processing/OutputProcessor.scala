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

import com.bwsw.common.ObjectSizeFetcher
import com.bwsw.sj.common.dal.model.stream.{ESStreamDomain, JDBCStreamDomain, RestStreamDomain, StreamDomain}
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.{Logger, LoggerFactory}

/**
  * This class used in [[com.bwsw.sj.engine.output.task.OutputTaskEngine]] for sending data to storage (storage could be different).
  * You should create concrete handler and realize delete() and send() methods.
  *
  * @param outputStream       stream indicating the specific storage
  * @param performanceMetrics set of metrics that characterize performance of an output streaming module
  */
abstract class OutputProcessor[T <: AnyRef](outputStream: StreamDomain,
                                            performanceMetrics: OutputStreamingPerformanceMetrics) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected def transactionFieldName: String = "txn"

  /**
    * Main method of handler: prepare, register and send.
    *
    * @param envelopes          list of processed envelopes from executor.
    * @param inputEnvelope      received envelope
    * @param wasFirstCheckpoint flag points whether the first checkpoint has been or not
    */
  def process(envelopes: Seq[OutputEnvelope],
              inputEnvelope: TStreamEnvelope[T],
              wasFirstCheckpoint: Boolean): Unit = {
    logger.debug("Process a set of envelopes that should be sent to output of specific type.")
    if (!wasFirstCheckpoint) delete(inputEnvelope)
    envelopes.foreach(envelope => registerAndSendEnvelope(envelope, inputEnvelope))
  }

  /**
    * Method for deleting data from storage. Must be implement.
    * It is used to avoid duplicates of envelopes
    *
    * @param envelope envelope to delete
    */
  def delete(envelope: TStreamEnvelope[T]): Unit

  def close(): Unit

  /**
    * Register envelope in performance metrics and then send to storage
    */
  private def registerAndSendEnvelope(outputEnvelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit = {
    registerOutputEnvelope(inputEnvelope.id.toString.replaceAll("-", ""), outputEnvelope)
    send(outputEnvelope, inputEnvelope)
  }

  /**
    * Register processed envelope in performance metrics.
    *
    * @param envelopeID envelope identifier
    * @param data       processed envelope
    */
  private def registerOutputEnvelope(envelopeID: String, data: OutputEnvelope): Unit = {
    logger.debug(s"Register an output envelope: '$envelopeID'.")
    val elementSize = ObjectSizeFetcher.getObjectSize(data)
    performanceMetrics.addElementToOutputEnvelope(outputStream.name, envelopeID, elementSize)
  }

  /**
    * Method for sending data to storage. Must be implement.
    *
    * @param envelope      processed envelope
    * @param inputEnvelope received envelope
    */
  protected def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit
}

object OutputProcessor {
  /**
    *
    * @param outputStream       specify a storage (storage info is stored in service:
    *                           [[com.bwsw.sj.common.dal.model.service.ESServiceDomain]],
    *                           [[com.bwsw.sj.common.dal.model.service.RestServiceDomain]],
    *                           [[com.bwsw.sj.common.dal.model.service.JDBCServiceDomain]])
    * @param performanceMetrics set of metrics that characterize performance of a batch streaming module
    * @param manager            allows to manage an environment of output streaming task
    * @param entity             user defined class to keep processed data
    * @tparam T type of elements that will be processed
    */
  def apply[T <: AnyRef](outputStream: StreamDomain,
                         performanceMetrics: OutputStreamingPerformanceMetrics,
                         manager: OutputTaskManager,
                         entity: Entity[_]): OutputProcessor[T] = {
    outputStream match {
      case esStreamDomain: ESStreamDomain =>
        new EsOutputProcessor[T](esStreamDomain, performanceMetrics, manager, entity)
      case jdbcStreamDomain: JDBCStreamDomain =>
        new JdbcOutputProcessor[T](jdbcStreamDomain, performanceMetrics, manager, entity)
      case restStreamDomain: RestStreamDomain =>
        new RestOutputProcessor[T](restStreamDomain, performanceMetrics, manager, entity)
    }
  }
}
