package com.bwsw.sj.engine.output

import com.bwsw.sj.engine.core.engine.{InstanceStatusObserver, TaskRunner}
import com.bwsw.sj.engine.output.task.{OutputTaskEngine, OutputTaskManager}
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
  * Runner object for engine of output-streaming module
  *
  * @author Kseniya Tomskikh
  */
object OutputTaskRunner extends {
  override val threadName = "OutputTaskRunner-%d"
} with TaskRunner {

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
