package com.bwsw.sj.engine.input

import java.io.File
import java.net.{InetSocketAddress, URLClassLoader}

import com.aerospike.client.Host
import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.ConfigConstants._
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.StreamConstants._
import com.bwsw.sj.engine.core.converter.ArrayByteConverter
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
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
import com.hazelcast.config.{EvictionPolicy, MaxSizeConfig, XmlConfigBuilder}
import com.hazelcast.core.Hazelcast
import org.slf4j.LoggerFactory

/**
 * Class allowing to manage an environment of input streaming task
 * Created: 08/07/2016
 *
 * @author Kseniya Mikhaleva
 */
class InputTaskManager() {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val instanceName = System.getenv("INSTANCE_NAME")
  val agentsHost = System.getenv("AGENTS_HOST")
  private val agentsPorts = System.getenv("AGENTS_PORTS").split(",")
  val taskName = System.getenv("TASK_NAME")
  val entryHost = System.getenv("ENTRY_HOST")
  val entryPort = System.getenv("ENTRY_PORT").toInt
  private val reportStream = instanceName + "_report"
  private var currentPortNumber = 0
  private val instance = ConnectionRepository.getInstanceService.get(instanceName).asInstanceOf[InputInstance]
  private val storage = ConnectionRepository.getFileStorage
  private val configService = ConnectionRepository.getConfigService

  private val transportTimeout = configService.get(transportTimeoutTag).value.toInt
  private val txnTTL = configService.get(txnTTLTag).value.toInt
  private val txnKeepAliveInterval = configService.get(txnKeepAliveIntervalTag).value.toInt
  private val producerKeepAliveInterval = configService.get(producerKeepAliveIntervalTag).value.toInt
  private val streamTTL = configService.get(streamTTLTag).value.toInt
  private val retryPeriod = configService.get(tgClientRetryPeriodTag).value.toInt
  private val retryCount = configService.get(tgRetryCountTag).value.toInt
  private val zkSessionTimeout = configService.get(zkSessionTimeoutTag).value.toInt
  private val zkConnectionTimeout = configService.get(zkConnectionTimeoutTag).value.toInt

  private val config = createHazelcastConfig()
  private val hazelcastInstance = Hazelcast.newHazelcastInstance(config)
  private val hazelcastMapName = "inputEngine"

  /**
   * Returns hazelcast map for checking of there are duplicates (input envelopes) or not
   * @return Hazelcast map
   */
  def getUniqueEnvelopes = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get hazelcast map for checking of there are duplicates (input envelopes) or not\n")
    hazelcastInstance.getMap[String, Array[Byte]](hazelcastMapName)
  }

  assert(agentsPorts.length >
    (instance.outputs.length + 1), //todo: count ! this one for pm
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
   * Creates a Hazelcast map configuration
   * @return Hazelcast map configuration
   */
  private def createHazelcastConfig() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create a Hazelcast map configuration is named 'inputEngine'\n")
    val config = new XmlConfigBuilder().build()
    val evictionPolicy = createEvictionPolicy()
    val maxSizeConfig = createMaxSizeConfig()

    config.getMapConfig(hazelcastMapName)
      .setTimeToLiveSeconds(instance.lookupHistory)
      .setEvictionPolicy(evictionPolicy)
      .setMaxSizeConfig(maxSizeConfig)

    config
  }

  /**
   * Creates an eviction policy for Hazelcast map configuration
   * @return Eviction policy
   */
  private def createEvictionPolicy() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create EvictionPolicy\n")
    instance.evictionPolicy match {
      case "LRU" => EvictionPolicy.LRU
      case "LFU" => EvictionPolicy.LFU
    }
  }

  /**
   * Creates a config that defines a max size of Hazelcast map
   * @return Max size configuration
   */
  private def createMaxSizeConfig() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create MaxSizeConfig\n")
    new MaxSizeConfig()
      .setSize(instance.queueMaxSize)
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
    instance.asInstanceOf[InputInstance]
  }

  /**
   * Returns instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor(inputEnvironmentManager: InputEnvironmentManager) = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    val moduleJar = getModuleJar
    val classLoader = getClassLoader(moduleJar.getAbsolutePath)
    val executor = classLoader.loadClass(fileMetadata.specification.executorClass)
      .getConstructor(classOf[InputEnvironmentManager])
      .newInstance(inputEnvironmentManager).asInstanceOf[InputStreamingExecutor]
    logger.debug(s"Task: $taskName. Create instance of executor class\n")

    executor
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
  def createOutputProducers = {
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
  def getReportStream = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get stream for performance metrics\n")
    getSjStream(
      reportStream,
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
  private def getSjStream(name: String, description: String, tags: Array[String], partitions: Int) = {
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
