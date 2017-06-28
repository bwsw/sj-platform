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
package com.bwsw.sj.engine.core.engine

import org.slf4j.LoggerFactory

/**
  * Provides methods for a basic execution logic of task engine
  * that has a checkpoint based on the number of messages (envelopes) [[com.bwsw.sj.common.utils.EngineLiterals.everyNthMode]]
  */

trait NumericalCheckpointTaskEngine {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var countOfEnvelopes = 0
  protected val checkpointInterval: Long
  private val isNotOnlyCustomCheckpoint = checkpointInterval > 0

  def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean = {
    isNotOnlyCustomCheckpoint && countOfEnvelopes == checkpointInterval || isCheckpointInitiated
  }

  def afterReceivingEnvelope(): Unit = {
    increaseCounter()
  }

  private def increaseCounter(): Unit = {
    countOfEnvelopes += 1
    logger.debug(s"Increase count of envelopes to: $countOfEnvelopes.")
  }

  def prepareForNextCheckpoint(): Unit = {
    resetCounter()
  }

  private def resetCounter(): Unit = {
    logger.debug(s"Reset a counter of envelopes to 0.")
    countOfEnvelopes = 0
  }
}
