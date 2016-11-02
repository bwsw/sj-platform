package com.bwsw.sj.engine.regular

import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.regular.task.engine.{RegularTaskEngine, RegularTaskEngineFactory}
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Object is responsible for running a task of job that launches regular module
 *
 *
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner extends {override val threadName = "RegularTaskRunner-%d"} with TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    try {
      val manager = new CommonTaskManager()

      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for regular module\n")

      val performanceMetrics: RegularStreamingPerformanceMetrics = new RegularStreamingPerformanceMetrics(manager)

      val regularTaskEngineFactory = new RegularTaskEngineFactory(manager, performanceMetrics)

      val regularTaskEngine: RegularTaskEngine = regularTaskEngineFactory.createRegularTaskEngine()

      val regularTaskInputService = regularTaskEngine.taskInputService

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(regularTaskInputService)
      executorService.submit(regularTaskEngine)
      executorService.submit(performanceMetrics)

      executorService.take().get()
    } catch {
      case assertionError: Error => handleException(assertionError)
      case exception: Exception => handleException(exception)
    }
  }
}