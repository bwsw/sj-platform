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
