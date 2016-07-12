package com.bwsw.sj.engine.regular

import java.io.File
import java.net.{InetSocketAddress, URLClassLoader}
import java.util.Properties

import com.aerospike.client.Host
import com.bwsw.common.ObjectSerializer
import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.ConfigConstants._
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.common.StreamConstants._
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.converter.ArrayByteConverter
import com.bwsw.sj.engine.core.environment.{ModuleEnvironmentManager, ModuleOutput}
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.regular.subscriber.RegularConsumerCallback
import com.bwsw.tstreams.agents.consumer.Offsets.{IOffset, Newest}
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.consumer.{BasicConsumer, BasicConsumerOptions, SubscriberCoordinationOptions}
import com.bwsw.tstreams.agents.producer.InsertionType.SingleElementInsert
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerOptions, ProducerCoordinationOptions}
import com.bwsw.tstreams.coordination.transactions.transport.impl.TcpTransport
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.data.cassandra.{CassandraStorageFactory, CassandraStorageOptions}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.BasicStream
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.JavaConverters._

/**
 * Class allowing to manage an environment of regular streaming task
 * Created: 13/04/2016
 *
 * @author Kseniya Mikhaleva
 */
class RegularTaskManager() {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val instanceName = System.getenv("INSTANCE_NAME")
  val agentsHost = System.getenv("AGENTS_HOST")
  private val agentsPorts = System.getenv("AGENTS_PORTS").split(",")
  val taskName = System.getenv("TASK_NAME")
  var kafkaOffsetsStorage = mutable.Map[(String, Int), Long]()
  private val kafkaOffsetsStream = taskName + "_kafka_offsets"
  private val stateStream = taskName + "_state"
  private val reportStream = instanceName + "_report"
  private var currentPortNumber = 0
  private val instance = ConnectionRepository.getInstanceService.get(instanceName)
  private val storage = ConnectionRepository.getFileStorage
  private val configService = ConnectionRepository.getConfigService

  val kafkaSubscriberTimeout = configService.get(kafkaSubscriberTimeoutTag).value.toInt
  private val txnPreload = configService.get(txnPreloadTag).value.toInt
  private val dataPreload = configService.get(dataPreloadTag).value.toInt
  private val consumerKeepAliveInterval = configService.get(consumerKeepAliveInternalTag).value.toInt
  private val transportTimeout = configService.get(transportTimeoutTag).value.toInt
  private val txnTTL = configService.get(txnTTLTag).value.toInt
  private val txnKeepAliveInterval = configService.get(txnKeepAliveIntervalTag).value.toInt
  private val producerKeepAliveInterval = configService.get(producerKeepAliveIntervalTag).value.toInt
  private val streamTTL = configService.get(streamTTLTag).value.toInt
  private val retryPeriod = configService.get(tgClientRetryPeriodTag).value.toInt
  private val retryCount = configService.get(tgRetryCountTag).value.toInt
  private val zkSessionTimeout = configService.get(zkSessionTimeoutTag).value.toInt
  private val zkConnectionTimeout = configService.get(zkConnectionTimeoutTag).value.toInt

  val inputs = instance.executionPlan.tasks.get(taskName).inputs.asScala
    .map(x => {
    val service = ConnectionRepository.getStreamService

    (service.get(x._1), x._2)
  })

  assert(agentsPorts.length >
    (inputs.count(x => x._1.streamType == StreamConstants.tStream) + instance.outputs.length + 3),
    "Not enough ports for t-stream consumers/producers ")

  private val fileMetadata: FileMetadata = ConnectionRepository.getFileMetadataService.getByParameters(
    Map("specification.name" -> instance.moduleName,
      "specification.module-type" -> instance.moduleType,
      "specification.version" -> instance.moduleVersion)
  ).head

  /**
   * Converter to convert usertype->storagetype; storagetype->usertype
   */
  private val converter = new ArrayByteConverter

  /**
   * An auxiliary service to retrieve settings of TStream providers
   */
  private val service = ConnectionRepository.getStreamService.get(instance.outputs.head).service.asInstanceOf[TStreamService]

  private val zkHosts = service.lockProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList

  /**
   * Metadata storage instance
   */
  private val metadataStorage: MetadataStorage = createMetadataStorage()

  /**
   * Creates metadata storage for producer/consumer settings
   */
  private def createMetadataStorage() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create metadata storage " +
      s"(namespace: ${service.metadataNamespace}, hosts: ${service.metadataProvider.hosts.mkString(",")}) " +
      s"for producer/consumer settings\n")
    val hosts = service.metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList
    (new MetadataStorageFactory).getInstance(
      cassandraHosts = hosts,
      keyspace = service.metadataNamespace)
  }

  /**
   * Creates data storage for producer/consumer settings
   */
  private def createDataStorage() = {
    service.dataProvider.providerType match {
      case "aerospike" =>
        logger.debug(s"Instance name: $instanceName, task name: $taskName. Create aerospike data storage " +
          s"(namespace: ${service.dataNamespace}, hosts: ${service.dataProvider.hosts.mkString(",")}) " +
          s"for producer/consumer settings\n")
        val options = new AerospikeStorageOptions(
          service.dataNamespace,
          service.dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toList)
        (new AerospikeStorageFactory).getInstance(options)

      case _ =>
        logger.debug(s"Instance name: $instanceName, task name: $taskName. Create cassandra data storage " +
          s"(namespace: ${service.dataNamespace}, hosts: ${service.dataProvider.hosts.mkString(",")}) " +
          s"for producer/consumer settings\n")
        val options = new CassandraStorageOptions(
          service.dataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList,
          service.dataNamespace
        )

        (new CassandraStorageFactory).getInstance(options)
    }
  }

  /**
   * Returns class loader for retrieving classes from jar
   *
   * @param pathToJar Absolute path to jar file
   * @return Class loader for retrieving classes from jar
   */
  private def getClassLoader(pathToJar: String) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get class loader for class: $pathToJar\n")
    val classLoaderUrls = Array(new File(pathToJar).toURI.toURL)

    new URLClassLoader(classLoaderUrls)

  }

  /**
   * Returns file contains uploaded module jar
   *
   * @return Local file contains uploaded module jar
   */
  private def getModuleJar: File = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get file contains uploaded '${instance.moduleName}' module jar\n")
    storage.get(fileMetadata.filename, s"tmp/${instance.moduleName}")
  }

  /**
   * Returns instance metadata to launch a module
   *
   * @return An instance metadata to launch a module
   */
  def getInstanceMetadata = {
    logger.info(s"Instance name: $instanceName, task name: $taskName. Get instance metadata\n")
    instance.asInstanceOf[RegularInstance]
  }

  /**
   * Returns instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor(moduleEnvironmentManager: ModuleEnvironmentManager) = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    val moduleJar = getModuleJar
    val classLoader = getClassLoader(moduleJar.getAbsolutePath)
    val executor = classLoader.loadClass(fileMetadata.specification.executorClass)
      .getConstructor(classOf[ModuleEnvironmentManager])
      .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]
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
   * Creates a t-stream producer for recording messages
   *
   * @param stream SjStream to which messages are written
   * @return Basic t-stream producer
   */
  def createProducer(stream: SjStream) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create basic producer for stream: ${stream.name}\n")
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    val coordinationOptions = new ProducerCoordinationOptions(
      agentAddress = agentsHost + ":" + agentsPorts(currentPortNumber),
      zkHosts,
      "/" + service.lockNamespace,
      zkSessionTimeout,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport,
      transportTimeout = transportTimeout,
      zkConnectionTimeout = zkConnectionTimeout
    )
    currentPortNumber += 1

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (0 until stream.asInstanceOf[TStreamSjStream].partitions).toList)

    val timeUuidGenerator =
      stream.asInstanceOf[TStreamSjStream].generator.generatorType match {
        case "local" => new LocalTimeUUIDGenerator
        case _type =>
          val service = stream.asInstanceOf[TStreamSjStream].generator.service.asInstanceOf[ZKService]
          val zkServers = service.provider.hosts
          val prefix = "/" + service.namespace + "/" + {
            if (_type == "global") _type else basicStream.name
          }

          new NetworkTimeUUIDGenerator(zkServers, prefix, retryPeriod, retryCount)
      }

    val options = new BasicProducerOptions[Array[Byte], Array[Byte]](
      txnTTL,
      txnKeepAliveInterval,
      producerKeepAliveInterval,
      roundRobinPolicy,
      SingleElementInsert,
      timeUuidGenerator,
      coordinationOptions,
      converter)

    new BasicProducer[Array[Byte], Array[Byte]](
      "producer_for_" + taskName + "_" + stream.name,
      basicStream,
      options
    )
  }

  /**
   * Create t-stream producers for each output stream
   * @return Map where key is stream name and value is t-stream producer
   */
  def createOutputProducers() = {
    logger.debug(s"Task: $taskName. Start creating t-stream producers for each output stream\n")
    val producers = instance.outputs
      .map(x => (x, ConnectionRepository.getStreamService.get(x)))
      .map(x => (x._1, createProducer(x._2))).toMap
    logger.debug(s"Task: $taskName. T-stream producers for each output stream are created\n")

    producers
  }

  /**
   * Creates t-stream to keep a module state or loads an existing t-stream
   *
   * @return SjStream used for keeping a module state
   */
  def getStateStream = {
    getTStream(stateStream, "store state of module", Array("state"), 1)
  }

  /**
   * Creates t-stream or loads an existing t-stream is responsible for committing the offsets of last messages
   * that has successfully processed for each topic for each partition
   *
   * @return t-stream is responsible for committing the offsets of last messages
   *         that has successfully processed for each topic for each partition
   */
  def getOffsetStream = {
    getTStream(kafkaOffsetsStream, "store kafka offsets of input streams", Array("offsets"), 1)
  }

  /**
   * Creates t-stream or loads an existing t-stream to keep the reports of module performance.
   * For each task there is specific partition (task number = partition number).
   *
   * @return SjStream used for keeping the reports of module performance
   */
  def getReportStream = {
    getTStream(
      reportStream,
      "store reports of performance metrics",
      Array("report", "performance"),
      instance.parallelism
    )
  }

  private def getTStream(name: String, description: String, tags: Array[String], partitions: Int) = {
    var stream: BasicStream[Array[Byte]] = null
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    if (BasicStreamService.isExist(name, metadataStorage)) {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
        s"Load t-stream: $name to $description\n")
      stream = BasicStreamService.loadStream(name, metadataStorage, dataStorage)
    } else {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
        s"Create t-stream: $name to $description\n")
      stream = BasicStreamService.createStream(
        name,
        partitions,
        streamTTL,
        description,
        metadataStorage,
        dataStorage
      )
    }

    new TStreamSjStream(
      stream.getName,
      stream.getDescriptions,
      stream.getPartitions,
      service,
      tStream,
      tags,
      new Generator("local")
    )
  }
}
