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
package com.bwsw.sj.engine.output

import java.io.Closeable
import java.util.concurrent.ExecutorCompletionService

import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.sj.engine.output.task.{OutputTaskEngine, OutputTaskManager}

/**
  * Class is responsible for launching output engine execution logic.
  * First, there are created all services needed to start engine. All of those services implement Callable interface
  * Next, each service are launched as a separate task using [[ExecutorCompletionService]]
  * Finally, handle a case if some task will fail and stop the execution. In other case the execution will go on indefinitely
  *
  * @author Kseniya Tomskikh
  */
object OutputTaskRunner extends {
  override val threadName = "OutputTaskRunner-%d"
} with TaskRunner {

  override protected def createTaskManager(): TaskManager = new OutputTaskManager()

  override protected def createPerformanceMetrics(manager: TaskManager): PerformanceMetrics = {
    new OutputStreamingPerformanceMetrics(manager.asInstanceOf[OutputTaskManager])
  }

  override protected def createTaskEngine(manager: TaskManager, performanceMetrics: PerformanceMetrics): TaskEngine =
    OutputTaskEngine(manager.asInstanceOf[OutputTaskManager], performanceMetrics.asInstanceOf[OutputStreamingPerformanceMetrics])

  override protected def createTaskInputService(manager: TaskManager, taskEngine: TaskEngine): Closeable = {
    taskEngine.asInstanceOf[OutputTaskEngine].taskInputService
  }
}
