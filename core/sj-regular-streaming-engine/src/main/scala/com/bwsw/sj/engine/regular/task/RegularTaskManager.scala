package com.bwsw.sj.engine.regular.task

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.environment.{RegularEnvironmentManager, EnvironmentManager, RegularModuleOutput}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.IOffset

import scala.collection.mutable

/**
 * Class allowing to manage an environment of regular streaming task
 *
 *
 * @author Kseniya Mikhaleva
 */
class RegularTaskManager() extends TaskManager {

  val outputTags = createOutputTags()

  assert(agentsPorts.length >=
    (inputs.count(x => x._1.streamType == StreamConstants.tStreamType) + instance.outputs.length + 3),
    "Not enough ports for t-stream consumers/producers." +
      s"${inputs.count(x => x._1.streamType == StreamConstants.tStreamType) + instance.outputs.length + 3} ports are required")

  /**
   * Returns instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    val executor = moduleClassLoader
      .loadClass(executorClassName)
      .getConstructor(classOf[RegularEnvironmentManager])
      .newInstance(environmentManager)
      .asInstanceOf[StreamingExecutor]
    logger.debug(s"Task: $taskName. Create instance of executor class\n")

    executor
  }

  private def createOutputTags() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get tags for each output stream\n")
    mutable.Map[String, (String, RegularModuleOutput)]()
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
      s"Create consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head})\n")

    val timeUuidGenerator = getUUIDGenerator(stream.asInstanceOf[TStreamSjStream])

    setStreamOptions(stream)

    tstreamFactory.getConsumer[Array[Byte]](
      "consumer_for_" + taskName + "_" + stream.name,
      timeUuidGenerator,
      converter,
      (0 until stream.asInstanceOf[TStreamSjStream].partitions).toList,
      offset)
  }
}
