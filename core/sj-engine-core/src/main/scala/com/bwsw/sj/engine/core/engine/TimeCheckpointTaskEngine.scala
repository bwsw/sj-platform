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

import com.bwsw.sj.common.utils.SjTimer
import org.slf4j.LoggerFactory

/**
  * Provides methods for a basic execution logic of task engine
  * that has a checkpoint based on time [[com.bwsw.sj.common.utils.EngineLiterals.timeIntervalMode]]
  */

trait TimeCheckpointTaskEngine {
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val checkpointInterval: Long

  private val checkpointTimer: Option[SjTimer] = createTimer()
  private val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  if (isNotOnlyCustomCheckpoint) setTimer()

  private def createTimer(): Option[SjTimer] = {
    if (checkpointInterval > 0) {
      logger.debug(s"Create a checkpoint timer.")
      Some(new SjTimer())
    } else {
      logger.debug(s"Input module has not programmatic checkpoint. Manually only.")
      None
    }
  }

  private def setTimer(): Unit = {
    checkpointTimer.get.set(checkpointInterval)
  }

  def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean = {
    isNotOnlyCustomCheckpoint && checkpointTimer.get.isTime || isCheckpointInitiated
  }

  def prepareForNextCheckpoint(): Unit = {
    resetTimer()
  }

  private def resetTimer(): Unit = {
    if (isNotOnlyCustomCheckpoint) {
      logger.debug(s"Prepare a checkpoint timer for next cycle.")
      checkpointTimer.get.reset()
      setTimer()
    }
  }

  def afterReceivingEnvelope(): Unit = {}
}
