package com.bwsw.sj.engine.core.managment

import java.io.File
import java.net.{InetSocketAddress, URLClassLoader}

import com.aerospike.client.Host
import com.bwsw.sj.common.ConfigConstants._
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.StreamConstants._
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.converter.ArrayByteConverter
import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.sj.engine.core.utils.EngineUtils
import com.bwsw.tstreams.agents.consumer.Offsets.IOffset
import com.bwsw.tstreams.agents.consumer.subscriber.{BasicSubscriberCallback, BasicSubscribingConsumer}
import com.bwsw.tstreams.agents.consumer.{BasicConsumerOptions, SubscriberCoordinationOptions}
import com.bwsw.tstreams.agents.producer.InsertionType.SingleElementInsert
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerOptions, ProducerCoordinationOptions}
import com.bwsw.tstreams.coordination.transactions.transport.impl.TcpTransport
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.data.cassandra.{CassandraStorageFactory, CassandraStorageOptions}
import com.bwsw.tstreams.generator.IUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.BasicStream
import org.slf4j.LoggerFactory

abstract class TaskManager() {
  protected val logger = LoggerFactory.getLogger(this.getClass)

  val streamDAO = ConnectionRepository.getStreamService

  val instanceName = System.getenv("INSTANCE_NAME")
  val agentsHost = System.getenv("AGENTS_HOST")
  protected val agentsPorts = System.getenv("AGENTS_PORTS").split(",")
  val taskName = System.getenv("TASK_NAME")
  protected val reportStreamName = instanceName + "_report"
  protected val instance: Instance = ConnectionRepository.getInstanceService.get(instanceName)
  protected var currentPortNumber = 0
  private val storage = ConnectionRepository.getFileStorage
  protected val configService = ConnectionRepository.getConfigService
  private val transportTimeout = configService.get(transportTimeoutTag).value.toInt
  private val txnTTL = configService.get(txnTTLTag).value.toInt
  private val txnPreload = configService.get(txnPreloadTag).value.toInt
  private val dataPreload = configService.get(dataPreloadTag).value.toInt
  private val txnKeepAliveInterval = configService.get(txnKeepAliveIntervalTag).value.toInt
  private val producerKeepAliveInterval = configService.get(producerKeepAliveIntervalTag).value.toInt
  private val consumerKeepAliveInterval = configService.get(consumerKeepAliveInternalTag).value.toInt
  private val streamTTL = configService.get(streamTTLTag).value.toInt
  protected val retryPeriod = configService.get(tgClientRetryPeriodTag).value.toInt
  protected val retryCount = configService.get(tgRetryCountTag).value.toInt
  protected val zkSessionTimeout = configService.get(zkSessionTimeoutTag).value.toInt
  protected val zkConnectionTimeout = configService.get(zkConnectionTimeoutTag).value.toInt
  protected val tStreamService = getTStreamService

  protected val zkHosts = tStreamService.lockProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList

  protected val fileMetadata: FileMetadata = ConnectionRepository.getFileMetadataService.getByParameters(
    Map("specification.name" -> instance.moduleName,
      "specification.module-type" -> instance.moduleType,
      "specification.version" -> instance.moduleVersion)
  ).head

  /**
   * Converter to convert usertype->storagetype; storagetype->usertype
   */
  val converter = new ArrayByteConverter

  /**
   * Metadata storage instance
   */
  val metadataStorage: MetadataStorage = createMetadataStorage()

  private val cassandraStorageFactory = new CassandraStorageFactory()
  private val aerospikeStorageFactory = new AerospikeStorageFactory()

  val outputProducers = createOutputProducers()
  val reportStream = getReportStream()

  /**
   * Creates metadata storage for producer/consumer settings
   */
  private def createMetadataStorage() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create metadata storage " +
      s"(namespace: ${tStreamService.metadataNamespace}, hosts: ${tStreamService.metadataProvider.hosts.mkString(",")}) " +
      s"for producer/consumer settings\n")
    val hosts = tStreamService.metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList
    (new MetadataStorageFactory).getInstance(
      cassandraHosts = hosts,
      keyspace = tStreamService.metadataNamespace)
  }

  /**
   * Creates data storage for producer/consumer settings
   */
  def createDataStorage() = {
    tStreamService.dataProvider.providerType match {
      case "aerospike" =>
        logger.debug(s"Instance name: $instanceName, task name: $taskName. Create aerospike data storage " +
          s"(namespace: ${tStreamService.dataNamespace}, hosts: ${tStreamService.dataProvider.hosts.mkString(",")}) " +
          s"for producer/consumer settings\n")
        val options = new AerospikeStorageOptions(
          tStreamService.dataNamespace,
          tStreamService.dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toList)
        aerospikeStorageFactory.getInstance(options)

      case _ =>
        logger.debug(s"Instance name: $instanceName, task name: $taskName. Create cassandra data storage " +
          s"(namespace: ${tStreamService.dataNamespace}, hosts: ${tStreamService.dataProvider.hosts.mkString(",")}) " +
          s"for producer/consumer settings\n")
        val options = new CassandraStorageOptions(
          tStreamService.dataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList,
          tStreamService.dataNamespace
        )

        cassandraStorageFactory.getInstance(options)
    }
  }

  /**
   * Creates an auxiliary service to retrieve settings of TStream providers
   */
  private def getTStreamService = {
    val streams = if (instance.inputs != null) {
      instance.outputs.union(instance.inputs.map(x => x.takeWhile(y => y != '/')))
    } else instance.outputs
    val sjStream = streams.map(s => streamDAO.get(s)).filter(s => s.streamType.equals(tStream)).head

    sjStream.service.asInstanceOf[TStreamService]
  }

  /**
   * Returns class loader for retrieving classes from jar
   *
   * @param pathToJar Absolute path to jar file
   * @return Class loader for retrieving classes from jar
   */
  protected def getClassLoader(pathToJar: String) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get class loader for class: $pathToJar\n")
    val classLoaderUrls = Array(new File(pathToJar).toURI.toURL)

    new URLClassLoader(classLoaderUrls)
  }

  /**
   * Returns file contains uploaded module jar
   *
   * @return Local file contains uploaded module jar
   */
  protected def getModuleJar: File = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get file contains uploaded '${instance.moduleName}' module jar\n")
    storage.get(fileMetadata.filename, s"tmp/${instance.moduleName}")
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
      "/" + tStreamService.lockNamespace,
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

    val timeUuidGenerator = EngineUtils.getUUIDGenerator(stream.asInstanceOf[TStreamSjStream])

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
   * Creates a t-stream consumer with pub/sub property
   *
   * @param stream SjStream from which massages are consumed
   * @param partitions Range of stream partition
   * @param offset Offset policy that describes where a consumer starts
   * @param callback Subscriber callback for t-stream consumer
   * @return T-stream subscribing consumer
   */
  def createSubscribingConsumer(stream: SjStream,
                                partitions: List[Int],
                                offset: IOffset,
                                callback: BasicSubscriberCallback[Array[Byte], Array[Byte]]) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create subscribing consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head})\n")

    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    val zkHosts = tStreamService.lockProvider.hosts.map { s =>
      val parts = s.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toList

    val agentAddress = agentsHost + ":" + agentsPorts(currentPortNumber)

    val coordinatorSettings = new SubscriberCoordinationOptions(
      agentAddress,
      s"/${tStreamService.lockNamespace}",
      zkHosts,
      zkSessionTimeout,
      zkConnectionTimeout
    )
    currentPortNumber += 1

    val basicStream = BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (partitions.head to partitions.tail.head).toList)

    val timeUuidGenerator: IUUIDGenerator = EngineUtils.getUUIDGenerator(stream.asInstanceOf[TStreamSjStream])

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      txnPreload,
      dataPreload,
      consumerKeepAliveInterval,
      converter,
      roundRobinPolicy,
      offset,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicSubscribingConsumer[Array[Byte], Array[Byte]](
      s"consumer_for_${taskName}_${stream.name}",
      basicStream,
      options,
      coordinatorSettings,
      callback,
      persistentQueuePath
    )
  }

  /**
   * Create t-stream producers for each output stream
   *
   * @return Map where key is stream name and value is t-stream producer
   */
  private def createOutputProducers() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create the basic t-stream producers for each output stream\n")

    instance.outputs
      .map(x => (x, ConnectionRepository.getStreamService.get(x)))
      .map(x => (x._1, createProducer(x._2))).toMap
  }

  /**
   * Creates a SjStream to keep the reports of module performance.
   * For each task there is specific partition (task number = partition number).
   *
   * @return SjStream used for keeping the reports of module performance
   */
  private def getReportStream(): TStreamSjStream = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get stream for performance metrics\n")

    getSjStream(
      reportStreamName,
      "store reports of performance metrics",
      Array("report", "performance"),
      instance.parallelism
    )
  }

  /**
   * Creates SjStream based on t-stream which is created or loaded
   *
   * @param name Name of t-stream
   * @param description Description of t-stream
   * @param tags Tags of t-stream
   * @param partitions Number of partitions of t-stream
   * @return SjStream with parameters described above
   */
  def getSjStream(name: String, description: String, tags: Array[String], partitions: Int) = {
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
      tStreamService,
      tStream,
      tags,
      new Generator("local")
    )
  }

  /**
   * Returns an instance metadata to launch a module
   *
   * @return An instance metadata to launch a module
   */
  def getInstanceMetadata: Instance = {
    logger.info(s"Instance name: $instanceName, task name: $taskName. Get instance metadata\n")

    instance
  }

  /**
   * Returns an instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor

  /**
   * Returns an instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor: StreamingExecutor
}
