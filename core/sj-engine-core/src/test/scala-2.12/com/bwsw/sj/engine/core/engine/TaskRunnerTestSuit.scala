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

import java.io.Closeable
import java.util.concurrent.{Callable, ExecutorCompletionService}

import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.common.engine.core.config.EngineConfigNames
import com.bwsw.sj.common.engine.core.managment.TaskManager
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals
import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

class TaskRunnerTestSuit extends FlatSpec with Matchers with TaskRunnerMocks {
  it should s"create all necessary services and launch them using ${classOf[ExecutorCompletionService[Unit]]}" in {
    //act
    taskRunner.main(Array())

    //assert
    verify(executorServiceMock, times(1)).submit(taskEngine)
    verify(executorServiceMock, times(1)).submit(performanceMetrics)
    verify(executorServiceMock, times(1)).submit(taskInputService)
    verify(executorServiceMock, times(4)).submit(any())
  }
}

trait TaskRunnerMocks extends MockitoSugar {
  private val config = mock[Config]
  when(config.getString(EngineConfigNames.taskName)).thenReturn("task-name")
  when(config.getString(any())).thenReturn("dummy")

  private val module = new Module {
    bind[Config] to config
  }

  private implicit val injector: Injector = module.injector

  val executorServiceMock: ExecutorCompletionService[Unit] = mock[ExecutorCompletionService[Unit]]
  when(executorServiceMock.submit(any())).thenReturn(null)

  private val instance = mock[Instance]
  when(instance.moduleType).thenReturn(EngineLiterals.regularStreamingType)

  val manager: TaskManager = mock[TaskManager]
  when(manager.taskName).thenReturn("task-name")
  when(manager.instance).thenReturn(instance)

  val performanceMetrics: PerformanceMetrics = mock[PerformanceMetrics]
  val taskEngine: TaskEngine = mock[TaskEngine]
  val taskInputService: TaskInputServiceMock = mock[TaskInputServiceMock]

  val taskRunner: TaskRunnerMock = new TaskRunnerMock(executorServiceMock,
    manager,
    performanceMetrics,
    taskEngine,
    taskInputService,
    injector)
}

class TaskRunnerMock(_executorService: ExecutorCompletionService[Unit],
                     _taskManager: TaskManager,
                     _performanceMetrics: PerformanceMetrics,
                     _taskEngine: TaskEngine,
                     _taskInputService: Closeable,
                     injector: Injector) extends {
  override protected val threadName: String = "name"
  override protected val executorService: ExecutorCompletionService[Unit] = _executorService
} with TaskRunner()(injector) with MockitoSugar {

  override def createTaskManager(): TaskManager = _taskManager

  override def createTaskInputService(manager: TaskManager, taskEngine: TaskEngine): Closeable = _taskInputService

  override def createPerformanceMetrics(manager: TaskManager): PerformanceMetrics = _performanceMetrics

  override def createTaskEngine(manager: TaskManager, performanceMetrics: PerformanceMetrics): TaskEngine = _taskEngine

  override protected def waitForCompletion(closeableTaskInput: Closeable): Unit = {}
}

class TaskInputServiceMock extends Callable[Unit] with Closeable {

  override def call(): Unit = ???

  override def close(): Unit = ???

}
