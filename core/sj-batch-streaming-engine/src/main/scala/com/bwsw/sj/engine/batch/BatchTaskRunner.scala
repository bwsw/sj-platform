package com.bwsw.sj.engine.batch

import java.util.concurrent.ExecutorCompletionService

import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.engine.batch.task.BatchTaskEngine
import com.bwsw.sj.engine.batch.task.input.{EnvelopeFetcher, RetrievableCheckpointTaskInput}
import com.bwsw.sj.engine.core.batch.BatchStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.engine.{InstanceStatusObserver, TaskRunner}
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.state.CommonModuleService
import org.slf4j.LoggerFactory

/**
  * Class is responsible for launching batch engine execution logic.
  * First, there are created all services needed to start engine. All of those services implement Callable interface
  * Next, each service are launched as a separate task using [[ExecutorCompletionService]]
  * Finally, handle a case if some task will fail and stop the execution. In other case the execution will go on indefinitely
  *
  * @author Kseniya Mikhaleva
  */
object BatchTaskRunner extends {
  override val threadName = "BatchTaskRunner-%d"
} with TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    val manager = new CommonTaskManager()
    val instance = manager.instance.asInstanceOf[BatchInstance]

    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for batch module\n")

    val taskInput = RetrievableCheckpointTaskInput[AnyRef](manager).asInstanceOf[RetrievableCheckpointTaskInput[Envelope]]
    val envelopeFetcher = new EnvelopeFetcher(taskInput)
    val performanceMetrics = new BatchStreamingPerformanceMetrics(manager)
    val moduleService = CommonModuleService(manager, envelopeFetcher.checkpointGroup, performanceMetrics)
    val batchCollector = manager.getBatchCollector(instance.to, performanceMetrics)

    val batchTaskEngine = new BatchTaskEngine(batchCollector, instance, moduleService, envelopeFetcher, performanceMetrics)

    val instanceStatusObserver = new InstanceStatusObserver(manager.instanceName)

    logger.info(s"Task: ${manager.taskName}. The preparation finished. Launch task\n")

    executorService.submit(batchTaskEngine)
    executorService.submit(performanceMetrics)
    executorService.submit(instanceStatusObserver)

    waitForCompletion(Some(taskInput))
  }
}
