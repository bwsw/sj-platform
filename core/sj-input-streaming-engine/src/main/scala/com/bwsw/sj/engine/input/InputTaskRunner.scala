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
package com.bwsw.sj.engine.input

import java.io.Closeable
import java.util.concurrent._

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.common.engine.core.managment.TaskManager
import com.bwsw.sj.common.engine.core.reporting.{PerformanceMetrics, PerformanceMetricsReporter}
import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.input.connection.tcp.server.{ChannelHandlerContextState, InputStreamingServer}
import com.bwsw.sj.engine.input.task.reporting.{InputStreamingPerformanceMetrics, InputStreamingPerformanceMetricsReporter}
import com.bwsw.sj.engine.input.task.{InputTaskEngine, InputTaskManager}
import io.netty.channel.ChannelHandlerContext
import scaldi.Injectable.inject

import scala.collection.JavaConverters._

/**
  * Class is responsible for launching input engine execution logic.
  * First, there are created all services needed to start engine. All of those services implement Callable interface
  * Next, each service are launched as a separate task using [[java.util.concurrent.ExecutorCompletionService]]
  * Finally, handle a case if some task will fail and stop the execution. Otherwise the execution will go on indefinitely
  *
  * @author Kseniya Mikhaleva
  */
object InputTaskRunner extends {
  override val threadName = "InputTaskRunner-%d"
} with TaskRunner {

  private val queueSize = 5000
  private val stateByContext = new ConcurrentHashMap[ChannelHandlerContext, ChannelHandlerContextState]().asScala
  private val channelContextQueue = new ArrayBlockingQueue[ChannelHandlerContext](queueSize)

  override protected def createTaskManager(): TaskManager = new InputTaskManager()

  override protected def createPerformanceMetricsReporter(manager: TaskManager): PerformanceMetricsReporter =
    new InputStreamingPerformanceMetricsReporter(manager.asInstanceOf[InputTaskManager])

  override protected def createTaskEngine(manager: TaskManager,
                                          performanceMetrics: PerformanceMetrics): TaskEngine = {
    InputTaskEngine(
      manager.asInstanceOf[InputTaskManager],
      performanceMetrics,
      channelContextQueue,
      stateByContext,
      inject[ConnectionRepository])
  }

  override protected def createTaskInputService(manager: TaskManager, taskEngine: TaskEngine): Closeable = {
    new InputStreamingServer(
      manager.agentsHost,
      manager.asInstanceOf[InputTaskManager].entryPort,
      channelContextQueue,
      stateByContext)
  }

  override protected def createPerformanceMetrics(taskName: String,
                                                  performanceMetricsReporter: PerformanceMetricsReporter): PerformanceMetrics = {
    new InputStreamingPerformanceMetrics(
      performanceMetricsReporter.asInstanceOf[InputStreamingPerformanceMetricsReporter],
      s"performance-metrics-$taskName")
  }
}
