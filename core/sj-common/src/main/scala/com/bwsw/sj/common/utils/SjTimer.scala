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
package com.bwsw.sj.common.utils

import java.util.{Timer, TimerTask}

import org.slf4j.LoggerFactory

/**
 * Class representing a wrapper for [[Timer]]
 *
 * @author Kseniya Mikhaleva
 */

class SjTimer {

  private val logger = LoggerFactory.getLogger(this.getClass)
  /**
   * Flag defines the timer went out or not
   */
  private var isTimerWentOut = false

  private val timer: Timer = new Timer()

  /**
   * Time when timer went out. Needed for computing lag between a real response time
   * and an invoke of time handler
   */
  var responseTime: Long = 0L

  /**
   * Sets a timer handler that changes flag on true value when time is went out
    *
    * @param delay delay in milliseconds before timer task is to be executed
   */
  def set(delay: Long): Unit = {
    logger.info(s"Set a timer to $delay.")
    timer.schedule(new TimerTask {
      def run() {
        isTimerWentOut = true
        responseTime = System.currentTimeMillis()
      }
    }, delay)
  }

  /**
   * Allows checking a timer has went out or not
 *
   * @return The result of checking
   */
  def isTime: Boolean = {
    logger.debug(s"Check whether a timer has went out or not.")
    isTimerWentOut
  }

  def reset(): Unit = {
    logger.debug(s"Reset a timer.")
    isTimerWentOut = false
    responseTime = 0
  }
}
