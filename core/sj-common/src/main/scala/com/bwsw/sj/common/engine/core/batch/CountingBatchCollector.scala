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
import com.bwsw.sj.common.engine.core.entities.Envelope
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

/**
  * Provides methods to gather batches that consists a certain amount of envelopes
  *
  * @param instance           set of settings of a batch streaming module
  * @param performanceMetrics set of metrics that characterize performance of a batch streaming module
  * @param inputs             contains input streams
  * @param batchSize          amount of envelopes per batch
  */
class CountingBatchCollector(instance: BatchInstanceDomain,
                             performanceMetrics: BatchStreamingPerformanceMetrics,
                             inputs: Array[StreamDomain],
                             batchSize: Int)
  extends BatchCollector(instance, performanceMetrics, inputs) {

  private val logger = Logger(this.getClass)
  private val countOfEnvelopesPerStream = mutable.Map(instance.getInputsWithoutStreamMode.map(x => (x, 0)): _*)

  def getBatchesToCollect(): Seq[String] =
    countOfEnvelopesPerStream.filter(_._2 == batchSize).keys.toSeq

  def afterEnvelopeReceive(envelope: Envelope): Unit = {
    countOfEnvelopesPerStream(envelope.stream) += 1
    logger.debug(
      s"Increase count of envelopes of stream: ${envelope.stream} to: ${countOfEnvelopesPerStream(envelope.stream)}.")
  }

  def prepareForNextCollecting(streamName: String): Unit = {
    logger.debug(s"Reset a counter of envelopes to 0.")
    countOfEnvelopesPerStream(streamName) = 0
  }
}
