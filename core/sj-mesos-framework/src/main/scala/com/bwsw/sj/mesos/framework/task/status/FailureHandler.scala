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
package com.bwsw.sj.mesos.framework.task.status

import com.bwsw.sj.mesos.framework.task.{TasksList, StatusHandler}
import org.apache.mesos.Protos.TaskStatus

object FailureHandler {

  def process(status: TaskStatus): Unit = {
    TasksList(status.getTaskId.getValue).foreach(task => task.update(node = "", reason = status.getMessage))
    TasksList(status.getTaskId.getValue).foreach(task => task.update(node = "", reason = status.getMessage))
    StatusHandler.logger.error(s"Error: ${status.getMessage}")

    TasksList.stopped(status.getTaskId.getValue)
    StatusHandler.logger.info(s"Added task ${status.getTaskId.getValue} to launch after failure.")
    TasksList.getTask(status.getTaskId.getValue).host = None
  }
}
