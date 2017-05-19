package com.bwsw.sj.engine.regular

import java.util.concurrent.ExecutorCompletionService

import com.bwsw.sj.engine.core.engine.{InstanceStatusObserver, TaskRunner}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.regular.task.RegularTaskEngine
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

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

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    val manager = new CommonTaskManager()

    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for regular module\n")

    val performanceMetrics = new RegularStreamingPerformanceMetrics(manager)

    val regularTaskEngine = RegularTaskEngine(manager, performanceMetrics)

    val regularTaskInputService = regularTaskEngine.taskInputService

    val instanceStatusObserver = new InstanceStatusObserver(manager.instanceName)

    logger.info(s"Task: ${manager.taskName}. The preparation finished. Launch task\n")

    executorService.submit(regularTaskInputService)
    executorService.submit(regularTaskEngine)
    executorService.submit(performanceMetrics)
    executorService.submit(instanceStatusObserver)

    waitForCompletion(Some(regularTaskInputService))
  }
}