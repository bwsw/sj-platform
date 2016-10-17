package com.bwsw.sj.engine.windowed

import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.engine.{WindowedTaskEngine, WindowedTaskEngineFactory}
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

object WindowedTaskRunner extends {override val threadName = "WindowedTaskRunner-%d"} with TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    try {
      val manager = new WindowedTaskManager()

      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for windowed module\n")

      val performanceMetrics = new WindowedStreamingPerformanceMetrics(manager)

      val regularTaskEngineFactory = new WindowedTaskEngineFactory(manager, performanceMetrics)

      val windowedTaskEngine: WindowedTaskEngine = regularTaskEngineFactory.createWindowedTaskEngine()

      val windowedTaskInputService: TaskInputService = windowedTaskEngine.taskInputService

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(windowedTaskInputService)
      executorService.submit(windowedTaskEngine)
      executorService.submit(performanceMetrics)

      executorService.take().get()
    } catch {
      case assertionError: Error => handleException(assertionError)
      case exception: Exception => handleException(exception)
    }
  }

}
