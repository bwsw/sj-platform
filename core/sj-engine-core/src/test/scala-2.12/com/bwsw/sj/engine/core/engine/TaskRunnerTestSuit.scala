package com.bwsw.sj.engine.core.engine

import java.io.Closeable
import java.util.concurrent.{Callable, ExecutorCompletionService}

import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.config.EngineConfigNames
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.typesafe.config.Config
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.Logger
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

  private val logger = mock[Logger]

  private val instance = mock[Instance]
  when(instance.moduleType).thenReturn(EngineLiterals.regularStreamingType)

  val manager: TaskManager = mock[TaskManager]
  when(manager.taskName).thenReturn("task-name")
  when(manager.instance).thenReturn(instance)

  val performanceMetrics: PerformanceMetrics = mock[PerformanceMetrics]
  val taskEngine: TaskEngine = mock[TaskEngine]
  val taskInputService: TaskInputServiceMock = mock[TaskInputServiceMock]

  val taskRunner: TaskRunnerMock = new TaskRunnerMock(executorServiceMock,
    logger,
    manager,
    performanceMetrics,
    taskEngine,
    taskInputService,
    injector)
}

class TaskRunnerMock(_executorService: ExecutorCompletionService[Unit],
                     _logger: Logger,
                     _taskManager: TaskManager,
                     _performanceMetrics: PerformanceMetrics,
                     _taskEngine: TaskEngine,
                     _taskInputService: Closeable,
                     injector: Injector) extends {
  override protected val threadName: String = "name"
  override protected val executorService: ExecutorCompletionService[Unit] = _executorService
  override protected val logger: Logger = _logger
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