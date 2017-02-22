package com.bwsw.sj.engine.windowed

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.engine.{InstanceStatusObserver, TaskRunner}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.reporting.WindowedStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.state.CommonModuleService
import com.bwsw.sj.engine.windowed.task.WindowedTaskEngine
import com.bwsw.sj.engine.windowed.task.input.EnvelopeFetcher
import org.slf4j.LoggerFactory

object WindowedTaskRunner extends {
  override val threadName = "WindowedTaskRunner-%d"
} with TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    try {
      val manager = new CommonTaskManager()
      val instance = manager.instance.asInstanceOf[WindowedInstance]

      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for windowed module\n")

      val envelopeFetcher = EnvelopeFetcher(manager)
      val performanceMetrics = new WindowedStreamingPerformanceMetrics(manager)
      val moduleService = CommonModuleService(manager, envelopeFetcher.checkpointGroup, performanceMetrics)
      val batchCollector = manager.getBatchCollector(instance, performanceMetrics)

      val windowedTaskEngine = new WindowedTaskEngine(batchCollector, instance, moduleService, envelopeFetcher, performanceMetrics)

      val instanceStatusObserver = new InstanceStatusObserver(manager.instanceName)

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

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
