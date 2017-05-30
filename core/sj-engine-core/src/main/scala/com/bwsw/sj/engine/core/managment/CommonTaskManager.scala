package com.bwsw.sj.engine.core.managment

import com.bwsw.sj.common.dal.model.instance.{BatchInstanceDomain, ExecutionPlan}
import com.bwsw.sj.common.dal.model.module.BatchSpecificationDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.Repository
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.common.si.model.instance.{BatchInstance, RegularInstance}
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import com.bwsw.sj.engine.core.batch.{BatchCollector, BatchStreamingPerformanceMetrics}
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, ModuleEnvironmentManager}
import com.bwsw.tstreams.agents.producer.Producer
import scaldi.Injector

import scala.collection.mutable

/**
  * Class allows to manage an environment of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] task
  *
  * @author Kseniya Mikhaleva
  */
class CommonTaskManager(implicit injector: Injector) extends TaskManager {
  val inputs: mutable.Map[StreamDomain, Array[Int]] = getInputs(getExecutionPlan())
  val outputProducers: Map[String, Producer] = createOutputProducers()

  require(numberOfAgentsPorts >=
    (inputs.count(x => x._1.streamType == StreamLiterals.tstreamType) + instance.outputs.length + 3),
    "Not enough ports for t-stream consumers/producers." +
      s"${inputs.count(x => x._1.streamType == StreamLiterals.tstreamType) + instance.outputs.length + 3} ports are required")

  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor = {
    logger.debug(s"Task: $taskName. Start loading an executor class from module jar.")
    val executor = executorClass
      .getConstructor(classOf[ModuleEnvironmentManager])
      .newInstance(environmentManager)
      .asInstanceOf[StreamingExecutor]
    logger.debug(s"Task: $taskName. Load an executor class.")

    executor
  }

  def getBatchCollector(instance: BatchInstanceDomain,
                        performanceMetrics: BatchStreamingPerformanceMetrics,
                        streamRepository: Repository[StreamDomain]): BatchCollector = {
    instance match {
      case _: BatchInstanceDomain =>
        logger.info(s"Task: $taskName. Getting a batch collector class from jar of file: " +
          instance.moduleType + "-" + instance.moduleName + "-" + instance.moduleVersion + ".")
        val batchCollectorClassName = fileMetadata.specification.asInstanceOf[BatchSpecificationDomain].batchCollectorClass
        val batchCollector = moduleClassLoader
          .loadClass(batchCollectorClassName)
          .getConstructor(
            classOf[BatchInstanceDomain],
            classOf[BatchStreamingPerformanceMetrics],
            classOf[Repository[StreamDomain]])
          .newInstance(instance, performanceMetrics, streamRepository)
          .asInstanceOf[BatchCollector]

        batchCollector
      case _ =>
        logger.error("A batch collector exists only for batch engine.")
        throw new RuntimeException("A batch collector exists only for batch engine.")
    }
  }

  private def getExecutionPlan(): ExecutionPlan = {
    logger.debug("Get an execution plan of instance.")
    instance match {
      case regularInstance: RegularInstance =>
        regularInstance.executionPlan
      case batchInstance: BatchInstance =>
        batchInstance.executionPlan
      case _ =>
        logger.error(s"CommonTaskManager can be used only for ${EngineLiterals.regularStreamingType} or ${EngineLiterals.batchStreamingType} engine.")
        throw new RuntimeException("CommonTaskManager can be used only for ${EngineLiterals.regularStreamingType} or ${EngineLiterals.batchStreamingType} engine.")
    }
  }
}