package com.bwsw.sj.engine.batch

import java.io.Closeable
import java.util.concurrent.ExecutorCompletionService

import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.engine.batch.task.BatchTaskEngine
import com.bwsw.sj.engine.core.batch.BatchStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.managment.{CommonTaskManager, TaskManager}
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics

/**
  * Class is responsible for launching batch engine execution logic.
  * First, there are created all services needed to start engine. All of those services implement Callable interface
  * Next, each service are launched as a separate task using [[ExecutorCompletionService]]
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
    new BatchTaskEngine(manager.asInstanceOf[CommonTaskManager], performanceMetrics.asInstanceOf[BatchStreamingPerformanceMetrics])
  }

  override protected def createTaskInputService(manager: TaskManager, taskEngine: TaskEngine): Closeable = {
    taskEngine.asInstanceOf[BatchTaskEngine].taskInputService
  }
}
