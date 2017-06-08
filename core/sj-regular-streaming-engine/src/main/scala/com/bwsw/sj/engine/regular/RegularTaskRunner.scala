package com.bwsw.sj.engine.regular

import java.io.Closeable
import java.util.concurrent.{Callable, ExecutorCompletionService}

import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.managment.{CommonTaskManager, TaskManager}
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.regular.task.RegularTaskEngine
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics

/**
  * Class is responsible for launching regular engine execution logic.
  * First, there are created all services needed to start engine. All of those services implement Callable interface
  * Next, each service are launched as a separate task using [[ExecutorCompletionService]]
  * Finally, handle a case if some task will fail and stop the execution. In other case the execution will go on indefinitely
  *
  * @author Kseniya Mikhaleva
  */
object RegularTaskRunner extends {
  override val threadName = "RegularTaskRunner-%d"
} with TaskRunner {

  override protected def createTaskManager(): TaskManager = new CommonTaskManager()

  override protected def createPerformanceMetrics(manager: TaskManager): PerformanceMetrics = {
    new RegularStreamingPerformanceMetrics(manager.asInstanceOf[CommonTaskManager])
  }

  override protected def createTaskEngine(manager: TaskManager, performanceMetrics: PerformanceMetrics): TaskEngine =
    RegularTaskEngine(manager.asInstanceOf[CommonTaskManager], performanceMetrics.asInstanceOf[RegularStreamingPerformanceMetrics])

  override protected def createTaskInputService(manager: TaskManager, taskEngine: TaskEngine): Callable[Unit] with Closeable = {
    taskEngine.asInstanceOf[RegularTaskEngine].taskInputService
  }
}