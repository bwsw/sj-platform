package com.bwsw.sj.engine.regular.task

import java.util.Properties

import com.bwsw.common.ObjectSerializer
import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.ConfigConstants._
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, ModuleEnvironmentManager, ModuleOutput}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.regular.subscriber.RegularConsumerCallback
import com.bwsw.tstreams.agents.consumer.Offsets.{IOffset, Newest}
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.consumer.{BasicConsumer, BasicConsumerOptions, SubscriberCoordinationOptions}
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.BasicStream
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Class allowing to manage an environment of regular streaming task
 * Created: 13/04/2016
 *
 * @author Kseniya Mikhaleva
 */
class RegularTaskManager() extends TaskManager {

  var kafkaOffsetsStorage = mutable.Map[(String, Int), Long]()
  private val kafkaOffsetsStream = taskName + "_kafka_offsets"
  private val stateStream = taskName + "_state"

  val kafkaSubscriberTimeout = configService.get(kafkaSubscriberTimeoutTag).value.toInt
  private val txnPreload = configService.get(txnPreloadTag).value.toInt
  private val dataPreload = configService.get(dataPreloadTag).value.toInt
  private val consumerKeepAliveInterval = configService.get(consumerKeepAliveInternalTag).value.toInt

  val inputs = instance.executionPlan.tasks.get(taskName).inputs.asScala
    .map(x => {
    val service = ConnectionRepository.getStreamService

    (service.get(x._1), x._2)
  })

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
   * Returns tags for each output stream
   *
   * @return
   */
  def getOutputTags = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get tags for each output stream\n")
    mutable.Map[String, (String, ModuleOutput)]()
  }

  /**
   * Creates a kafka consumer for all input streams of kafka type.
   * If there was a checkpoint with offsets of last consumed messages for each topic/partition
   * then consumer will fetch from this offsets otherwise in accordance with offset parameter
   *
   * @param topics Set of kafka topic names and range of partitions relatively
   * @param hosts Addresses of kafka brokers in host:port format
   * @param offset Default policy for kafka consumer (earliest/latest)
   * @return Kafka consumer subscribed to topics
   */
  def createKafkaConsumer(topics: List[(String, List[Int])], hosts: List[String], offset: String): KafkaConsumer[Array[Byte], Array[Byte]] = {
    import collection.JavaConverters._
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create kafka consumer for topics (with their partitions): " +
      s"${topics.map(x => s"topic name: ${x._1}, " + s"partitions: ${x._2.mkString(",")}").mkString(",")}\n")
    val objectSerializer = new ObjectSerializer()
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    val props = new Properties()
    props.put("bootstrap.servers", hosts.mkString(","))
    props.put("enable.auto.commit", "false")
    props.put("auto.offset.reset", offset)
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    configService.getByParameters(Map("domain" -> "kafka")).foreach(x => props.put(x.name.replace(x.domain + ".", ""), x.value))

    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)

    val topicPartitions = topics.flatMap(x => {
      (x._2.head to x._2.tail.head).map(y => new TopicPartition(x._1, y))
    }).asJava

    consumer.assign(topicPartitions)

    if (BasicStreamService.isExist(kafkaOffsetsStream, metadataStorage)) {
      val stream = BasicStreamService.loadStream(kafkaOffsetsStream, metadataStorage, dataStorage)
      val roundRobinPolicy = new RoundRobinPolicy(stream, (0 to 0).toList)
      val timeUuidGenerator = new LocalTimeUUIDGenerator
      val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
        txnPreload,
        dataPreload,
        consumerKeepAliveInterval,
        converter,
        roundRobinPolicy,
        Newest,
        timeUuidGenerator,
        useLastOffset = true)

      val offsetConsumer = new BasicConsumer[Array[Byte], Array[Byte]]("consumer for offsets of " + taskName, stream, options)
      val lastTxn = offsetConsumer.getLastTransaction(0)

      if (lastTxn.isDefined) {
        logger.debug(s"Instance name: $instanceName, task name: $taskName. Get saved offsets for kafka consumer and apply their\n")
        kafkaOffsetsStorage = objectSerializer.deserialize(lastTxn.get.next()).asInstanceOf[mutable.Map[(String, Int), Long]]
        kafkaOffsetsStorage.foreach(x => consumer.seek(new TopicPartition(x._1._1, x._1._2), x._2 + 1))
      }
    }

    consumer
  }

  /**
   * Creates a t-stream consumer with pub/sub property
   *
   * @param stream SjStream from which massages are consumed
   * @param partitions Range of stream partition
   * @param offset Offset policy that describes where a consumer starts
   * @param queue Queue which keeps consumed messages
   * @return T-stream subscribing consumer
   */
  def createSubscribingConsumer(stream: SjStream, partitions: List[Int], offset: IOffset, queue: PersistentBlockingQueue) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create subscribing consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head})\n")
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    val coordinatorSettings = new SubscriberCoordinationOptions(
      agentsHost + ":" + agentsPorts(currentPortNumber),
      service.lockNamespace,
      zkHosts,
      zkSessionTimeout,
      zkConnectionTimeout
    )
    currentPortNumber += 1

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (partitions.head to partitions.tail.head).toList)

    val timeUuidGenerator =
      stream.asInstanceOf[TStreamSjStream].generator.generatorType match {
        case "local" => new LocalTimeUUIDGenerator
        case _type =>
          val service = stream.asInstanceOf[TStreamSjStream].generator.service.asInstanceOf[ZKService]
          val zkHosts = service.provider.hosts
          val prefix = "/" + service.namespace + "/" + {
            if (_type == "global") _type else stream.name
          }

          new NetworkTimeUUIDGenerator(zkHosts, prefix, retryPeriod, retryCount)
      }

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      txnPreload,
      dataPreload,
      consumerKeepAliveInterval,
      converter,
      roundRobinPolicy,
      offset,
      timeUuidGenerator,
      useLastOffset = true)

    val callback = new RegularConsumerCallback[Array[Byte], Array[Byte]](queue)

    new BasicSubscribingConsumer[Array[Byte], Array[Byte]](
      "consumer_for_" + taskName + "_" + stream.name,
      basicStream,
      options,
      coordinatorSettings,
      callback,
      persistentQueuePath
    )
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
  def getStateStream = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get stream for keeping state of module\n")
    getSjStream(stateStream, "store state of module", Array("state"), 1)
  }

  /**
   * Creates SJStream is responsible for committing the offsets of last messages
   * that has successfully processed for each topic for each partition
   *
   * @return SJStream is responsible for committing the offsets of last messages
   *         that has successfully processed for each topic for each partition
   */
  def getOffsetStream = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get stream for keeping kafka offsets\n")
    getSjStream(kafkaOffsetsStream, "store kafka offsets of input streams", Array("offsets"), 1)
  }
}
