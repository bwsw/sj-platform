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
package com.bwsw.sj.engine.batch

import java.io.Closeable

import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.common.engine.core.batch.BatchStreamingPerformanceMetrics
import com.bwsw.sj.common.engine.core.managment.{CommonTaskManager, TaskManager}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.batch.task.BatchTaskEngine
import com.bwsw.sj.engine.core.engine.TaskRunner
import scaldi.Injectable.inject

/**
  * Class is responsible for launching batch engine execution logic.
  * First, there are created all services needed to start engine. All of those services implement Callable interface
  * Next, each service are launched as a separate task using [[java.util.concurrent.ExecutorCompletionService]]
  * Finally, handle a case if some task will fail and stop the execution. In other case the execution will go on indefinitely
  *
  * @author Kseniya Mikhaleva
  */
object BatchTaskRunner extends {
  override val threadName = "BatchTaskRunner-%d"
} with TaskRunner {

  override protected def createTaskManager(): TaskManager = new CommonTaskManager()

  override protected def createPerformanceMetrics(manager: TaskManager): PerformanceMetrics = {
    new BatchStreamingPerformanceMetrics(manager.asInstanceOf[CommonTaskManager])
  }

  override protected def createTaskEngine(manager: TaskManager, performanceMetrics: PerformanceMetrics): TaskEngine = {
    val lowWatermark = inject[SettingsUtils].getLowWatermark()
    new BatchTaskEngine(manager.asInstanceOf[CommonTaskManager], performanceMetrics.asInstanceOf[BatchStreamingPerformanceMetrics], lowWatermark)
  }

  override protected def createTaskInputService(manager: TaskManager, taskEngine: TaskEngine): Closeable = {
    taskEngine.asInstanceOf[BatchTaskEngine].taskInputService
  }
}

class BatchTaskRunner
