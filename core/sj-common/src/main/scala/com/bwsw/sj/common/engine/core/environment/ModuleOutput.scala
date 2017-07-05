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
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.tstreams.agents.producer.ProducerTransaction
import org.slf4j.{Logger, LoggerFactory}

/**
  * Common class that is a wrapper for output stream
  *
  * @param performanceMetrics set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]]
  *                           or [[EngineLiterals.batchStreamingType]] module
  */
abstract class ModuleOutput(private val performanceMetrics: PerformanceMetrics) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected def updatePerformanceMetrics(streamName: String, txn: ProducerTransaction, bytes: Array[Byte]): Unit = {
    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'.")

    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      txn.getTransactionID.toString,
      bytes.length
    )
  }

  def clear(): Unit
}
