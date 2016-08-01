package com.bwsw.sj.engine.regular.task

import com.bwsw.sj.common.ConfigConstants._
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, ModuleEnvironmentManager, ModuleOutput}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.tstreams.agents.consumer.Offsets.IOffset
import com.bwsw.tstreams.agents.consumer.{BasicConsumer, BasicConsumerOptions}
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.BasicStream

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Class allowing to manage an environment of regular streaming task
 * Created: 13/04/2016
 *
 * @author Kseniya Mikhaleva
 */
class RegularTaskManager() extends TaskManager {

  private val stateStreamName = taskName + "_state"
  val txnPreload = configService.get(txnPreloadTag).value.toInt
  val dataPreload = configService.get(dataPreloadTag).value.toInt
  val consumerKeepAliveInterval = configService.get(consumerKeepAliveInternalTag).value.toInt

  val inputs = instance.executionPlan.tasks.get(taskName).inputs.asScala
    .map(x => {
    val service = ConnectionRepository.getStreamService

    (service.get(x._1), x._2)
  })

  val stateStream = createStateStream()
  val outputTags = createOutputTags()

  assert(agentsPorts.length >=
    (inputs.count(x => x._1.streamType == StreamConstants.tStream) + instance.outputs.length + 3),
    "Not enough ports for t-stream consumers/producers ")

  /**
   * Returns instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    val moduleJar = getModuleJar
    val classLoader = getClassLoader(moduleJar.getAbsolutePath)
    val executor = classLoader.loadClass(fileMetadata.specification.executorClass)
      .getConstructor(classOf[ModuleEnvironmentManager])
      .newInstance(environmentManager).asInstanceOf[RegularStreamingExecutor]
    logger.debug(s"Task: $taskName. Create instance of executor class\n")

    executor
  }

  /**
    * Returns an instance of executor of module
    *
    * @return An instance of executor of module
    */
  def getExecutor: StreamingExecutor = ???

  /**
   * Returns tags for each output stream
   *
   * @return
   */
  private def createOutputTags() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get tags for each output stream\n")
    mutable.Map[String, (String, ModuleOutput)]()
  }

  /**
   * Creates an ordinary t-stream consumer
   *
   * @param stream SjStream from which massages are consumed
   * @param partitions Range of stream partition
   * @param offset Offset policy that describes where a consumer starts
   * @return Basic t-stream consumer
   */
  def createConsumer(stream: SjStream, partitions: List[Int], offset: IOffset): BasicConsumer[Array[Byte], Array[Byte]] = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create basic consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head})\n")
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (partitions.head to partitions.tail.head).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      txnPreload,
      dataPreload,
      consumerKeepAliveInterval,
      converter,
      roundRobinPolicy,
      offset,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicConsumer[Array[Byte], Array[Byte]](
      "consumer_for_" + taskName + "_" + stream.name,
      basicStream,
      options
    )
  }

  /**
   * Creates SJStream to keep a module state
   *
   * @return SjStream used for keeping a module state
   */
  private def createStateStream() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get stream for keeping state of module\n")
    getSjStream(stateStreamName, "store state of module", Array("state"), 1)
  }
}
