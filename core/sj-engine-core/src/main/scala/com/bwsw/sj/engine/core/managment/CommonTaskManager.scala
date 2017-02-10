package com.bwsw.sj.engine.core.managment

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{RegularInstance, WindowedInstance}
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, ModuleEnvironmentManager}
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.IOffset

import scala.collection.mutable

/**
 * Class allowing to manage an environment of regular streaming task
 *
 *
 * @author Kseniya Mikhaleva
 */
class CommonTaskManager() extends TaskManager {
  private val executorInstance = executorClass.getConstructor(classOf[ModuleEnvironmentManager])
    .newInstance(new EnvironmentManager(Map(), Array()))
  val _type = executorInstance.getClass.getMethod("getType").invoke(executorInstance).asInstanceOf[_root_.scala.reflect.runtime.universe.Type]
  val inputs: mutable.Map[SjStream, Array[Int]] = getInputs(getExecutionPlan)
  val outputProducers = createOutputProducers()

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

  /**
   * Creates a t-stream consumer
   *
   * @param stream SjStream from which massages are consumed
   * @param partitions Range of stream partition
   * @param offset Offset policy that describes where a consumer starts
   * @return T-stream consumer
   */
  def createConsumer(stream: TStreamSjStream, partitions: List[Int], offset: IOffset): Consumer[Array[Byte]] = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head}).")

    val transactionGenerator = getTransactionGenerator(stream)

    setStreamOptions(stream)

    tstreamFactory.getConsumer[Array[Byte]](
      "consumer_for_" + taskName + "_" + stream.name,
      transactionGenerator,
      converter,
      (0 until stream.partitions).toSet,
      offset)
  }

  private def getExecutionPlan() = {
    logger.debug("Get an execution plan of instance.")
    instance match {
      case regularInstance: RegularInstance =>
        regularInstance.executionPlan
      case windowedInstance: WindowedInstance =>
        windowedInstance.executionPlan
      case _ =>
        logger.error("CommonTaskManager can be used only for regular or windowed engine.")
        throw new RuntimeException("CommonTaskManager can be used only for regular or windowed engine.")
    }
  }
}
