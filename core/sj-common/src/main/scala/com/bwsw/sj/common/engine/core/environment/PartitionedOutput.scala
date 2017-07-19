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
package com.bwsw.sj.common.engine.core.environment

import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewProducerTransactionPolicy, Producer, ProducerTransaction}

import scala.collection._

/**
  * Provides an output stream that defined for each partition
  *
  * @param producer           producer of specific output
  * @param performanceMetrics set of metrics that characterize performance
  *                           of [[com.bwsw.sj.common.utils.EngineLiterals.regularStreamingType]]
  *                           or [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] module
  * @author Kseniya Mikhaleva
  */

class PartitionedOutput(producer: Producer,
                        performanceMetrics: PerformanceMetrics)
                       (implicit serialize: AnyRef => Array[Byte])
  extends ModuleOutput(performanceMetrics) {

  private val transactions = mutable.Map[Int, ProducerTransaction]()
  private val streamName = producer.stream.name

  def put(data: AnyRef, partition: Int): Unit = {
    val bytes = serialize(data)
    logger.debug(s"Send a portion of data to stream: '$streamName' partition with number: '$partition'.")
    if (transactions.contains(partition)) {
      transactions(partition).send(bytes)
    }
    else {
      transactions(partition) = producer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened, partition)
      transactions(partition).send(bytes)
    }

    updatePerformanceMetrics(streamName, transactions(partition), bytes)
  }

  override def clear(): Unit = {
    transactions.clear()
  }
}