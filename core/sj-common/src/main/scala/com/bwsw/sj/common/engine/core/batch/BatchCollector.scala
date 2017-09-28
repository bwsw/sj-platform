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
package com.bwsw.sj.common.engine.core.batch

import com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.engine.core.entities.{Batch, Envelope}
import com.typesafe.scalalogging.Logger

import scala.collection.Map

/**
  * Provides methods to gather batches that consist of envelopes
  *
  * @param instance           set of settings of a batch streaming module
  * @param performanceMetrics set of metrics that characterize performance of a batch streaming module
  * @author Kseniya Mikhaleva
  */
abstract class BatchCollector(protected val instance: BatchInstanceDomain,
                              performanceMetrics: BatchStreamingPerformanceMetricsProxy,
                              inputs: Array[StreamDomain]) {

  private val logger: Logger = Logger(this.getClass)
  private val currentBatchPerStream: Map[String, Batch] = createStorageOfBatches()

  private def createStorageOfBatches(): Map[String, Batch] = {
    inputs.map(x => (x.name, new Batch(x.name, x.tags))).toMap
  }

  final def onReceive(envelope: Envelope): Unit = {
    logger.debug(s"Invoke onReceive() handler.")
    registerEnvelope(envelope)
    afterEnvelopeReceive(envelope)
  }

  private def registerEnvelope(envelope: Envelope): Unit = {
    logger.debug(s"Register an envelope: ${envelope.toString}.")
    currentBatchPerStream(envelope.stream).envelopes += envelope
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

  final def collectBatch(streamName: String): Batch = {
    logger.info(s"It's time to collect batch (stream: $streamName)\n")
    val batch = currentBatchPerStream(streamName).copy()
    currentBatchPerStream(streamName).envelopes.clear()
    prepareForNextCollecting(streamName)

    batch
  }

  protected def afterEnvelopeReceive(envelope: Envelope): Unit

  def getBatchesToCollect(): Seq[String]

  protected def prepareForNextCollecting(streamName: String): Unit
}