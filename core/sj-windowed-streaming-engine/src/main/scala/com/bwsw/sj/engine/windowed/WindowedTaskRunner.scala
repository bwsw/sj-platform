package com.bwsw.sj.engine.windowed

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.{InstanceStatusObserver, TaskRunner}
import com.bwsw.sj.engine.core.entities.{Batch, Envelope}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.windowed.batch.BatchCollector
import com.bwsw.sj.engine.windowed.task.WindowedTaskEngine
import com.bwsw.sj.engine.windowed.task.input.RetrievableTaskInput
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
      val inputService = RetrievableTaskInput[manager._type.type](manager).asInstanceOf[RetrievableTaskInput[Envelope]]

      val batchCollector = BatchCollector[manager._type.type](manager, inputService, batchQueue, performanceMetrics)

      val windowedTaskEngine = new WindowedTaskEngine(manager, inputService, batchQueue, performanceMetrics)

      val instanceStatusObserver = new InstanceStatusObserver(manager.instanceName)

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(batchCollector)
      executorService.submit(windowedTaskEngine)
      executorService.submit(performanceMetrics)
      executorService.submit(instanceStatusObserver)

      executorService.take().get()
    } catch {
      case requiringError: IllegalArgumentException => handleException(requiringError)
      case exception: Exception => handleException(exception)
    }
  }

}
