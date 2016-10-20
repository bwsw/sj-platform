package com.bwsw.sj.engine.windowed

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.core.entities.Batch
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.engine.collecting.BatchCollector
import com.bwsw.sj.engine.windowed.task.engine.WindowedTaskEngine
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

object WindowedTaskRunner extends {
  override val threadName = "WindowedTaskRunner-%d"
} with TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    try {
      val manager = new WindowedTaskManager()

      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for windowed module\n")

      val performanceMetrics = new WindowedStreamingPerformanceMetrics(manager)
      val batchQueue: ArrayBlockingQueue[Batch] = new ArrayBlockingQueue(1000)

      val batchCollector = new BatchCollector(manager, batchQueue, performanceMetrics)
      val windowedTaskEngine = new WindowedTaskEngine(manager, batchQueue, performanceMetrics)

      val windowedTaskInputService: TaskInputService = batchCollector.taskInputService

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(windowedTaskInputService)
      executorService.submit(batchCollector)
      executorService.submit(windowedTaskEngine)
      //executorService.submit(performanceMetrics)

      executorService.take().get()
    } catch {
      case assertionError: Error => handleException(assertionError)
      case exception: Exception => handleException(exception)
    }
  }

}
