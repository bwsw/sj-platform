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
package com.bwsw.sj.mesos.framework.task

import com.bwsw.common.JsonSerializer
import org.apache.log4j.Logger
import org.apache.mesos.Protos._
import com.bwsw.sj.mesos.framework.task.status._

/**
  * Handler for mesos task status.
  */
object StatusHandler {
  val logger = Logger.getLogger(getClass)
  val serializer = new JsonSerializer()

  /**
    * Determines type of status and restarts task, if status "failed" or "error"
 *
    * @param status: mesos task status
    */
  def handle(status: TaskStatus): Unit = {

    if (Option(status).isDefined) {

      TasksList(status.getTaskId.getValue).foreach(task => task.update(
        state = status.getState.toString,
        stateChanged = status.getTimestamp.toLong * 1000,
        lastNode = if (task.node != "") task.node else task.lastNode,
        node = status.getSlaveId.getValue
      ))

      logger.debug(s"Task: ${status.getTaskId.getValue}.")
      logger.info(s"Status: ${status.getState}.")

      status.getState.toString match {
        case "TASK_FAILED" | "TASK_ERROR" => FailureHandler.process(status)
        case "TASK_RUNNING" => SuccessHandler.process(status)
        case "TASK_KILLED" => KilledHandler.process(status)
        case "TASK_LOST" => LostHandler.process(status)
        case _ =>
      }
    }
  }
}








