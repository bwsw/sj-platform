package com.bwsw.sj.engine.windowed

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.entities.Batch
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.windowed.task.engine.input.InputFactory
import com.bwsw.sj.engine.windowed.task.engine.{BatchCollectorFactory, WindowedTaskEngine}
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

object WindowedTaskRunner extends {
  override val threadName = "WindowedTaskRunner-%d"
} with TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val batchQueue: ArrayBlockingQueue[Batch] = new ArrayBlockingQueue(EngineLiterals.queueSize)

  def main(args: Array[String]) {
    try {
      val manager = new CommonTaskManager()

      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for windowed module\n")

      val performanceMetrics = new WindowedStreamingPerformanceMetrics(manager)
      val inputService = new InputFactory(manager).createInputService()

      val batchCollector = new BatchCollectorFactory(manager, inputService, batchQueue, performanceMetrics).createBatchCollector()
      val windowedTaskEngine = new WindowedTaskEngine(manager, inputService, batchQueue, performanceMetrics)

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(batchCollector)
      executorService.submit(windowedTaskEngine)
      executorService.submit(performanceMetrics)

      executorService.take().get()
    } catch {
      case assertionError: Error => handleException(assertionError)
      case exception: Exception => handleException(exception)
    }
  }

}
