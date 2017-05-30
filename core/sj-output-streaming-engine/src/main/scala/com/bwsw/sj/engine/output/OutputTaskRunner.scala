package com.bwsw.sj.engine.output

import java.util.concurrent.ExecutorCompletionService

import com.bwsw.sj.engine.core.engine.{InstanceStatusObserver, TaskRunner}
import com.bwsw.sj.engine.output.task.{OutputTaskEngine, OutputTaskManager}
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

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

  import com.bwsw.sj.common.SjModule._

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    val manager = new OutputTaskManager()

    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for output module\n")

    val performanceMetrics = new OutputStreamingPerformanceMetrics(manager)

    val outputTaskEngine = OutputTaskEngine(manager, performanceMetrics)

    val outputTaskInputService = outputTaskEngine.taskInputService

    val instanceStatusObserver = new InstanceStatusObserver(manager.instanceName)

    logger.info(s"Task: ${manager.taskName}. The preparation finished. Launch task\n")

    executorService.submit(outputTaskInputService)
    executorService.submit(outputTaskEngine)
    executorService.submit(performanceMetrics)
    executorService.submit(instanceStatusObserver)

    waitForCompletion(Some(outputTaskInputService))
  }
}
