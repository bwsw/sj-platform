package com.bwsw.sj.engine.output

import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.engine.OutputTaskEngineFactory
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Runner object for engine of output-streaming module
 *
 *
 * @author Kseniya Tomskikh
 */
object OutputTaskRunner extends {override val threadName = "OutputTaskRunner-%d"} with TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    try {
      val manager = new OutputTaskManager()

      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for output module\n")

      val performanceMetrics = new OutputStreamingPerformanceMetrics(manager)

      val outputTaskEngineFactory = new OutputTaskEngineFactory(manager, performanceMetrics)

      val outputTaskEngine = outputTaskEngineFactory.createOutputTaskEngine()

      val outputTaskInputService: TaskInputService = outputTaskEngine.taskInputService

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(outputTaskInputService)
      executorService.submit(outputTaskEngine)
      executorService.submit(performanceMetrics)

      executorService.take().get()
    } catch {
      case assertionError: Error => handleException(assertionError)
      case exception: Exception => handleException(exception)
    }
  }
}