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
package com.bwsw.sj.benchmarks.module.batch

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.benchmark.BatchExecutorOptions
import com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.engine.core.batch.{BatchCollector, BatchStreamingPerformanceMetrics}
import com.bwsw.sj.common.engine.core.entities.Envelope

import scala.collection.mutable

/**
  * @author Pavel Tomskikh
  */
class TimeBatchCollector(instance: BatchInstanceDomain,
                         performanceMetrics: BatchStreamingPerformanceMetrics,
                         inputs: Array[StreamDomain])
  extends BatchCollector(instance, performanceMetrics, inputs) {

  private val jsonSerializer = new JsonSerializer(ignoreUnknown = true)
  private val options = jsonSerializer.deserialize[BatchExecutorOptions](instance.options)
  private val batchSize = options.batchSize
  private var lastTimestamp: Long = System.currentTimeMillis()

  private val streamHasEnvelopes: mutable.Map[String, Boolean] =
    mutable.Map(instance.getInputsWithoutStreamMode.map(stream => stream -> false): _*)


  override def getBatchesToCollect(): Seq[String] = {
    val currentTimestamp = System.currentTimeMillis()

    if (currentTimestamp - lastTimestamp >= batchSize) {
      lastTimestamp = currentTimestamp

      streamHasEnvelopes.filter(_._2).keySet.toSeq
    } else
      Seq.empty
  }

  override protected def afterEnvelopeReceive(envelope: Envelope): Unit = {}

  override protected def prepareForNextCollecting(streamName: String): Unit =
    streamHasEnvelopes(streamName) = false
}
