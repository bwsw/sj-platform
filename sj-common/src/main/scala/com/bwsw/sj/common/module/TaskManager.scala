package com.bwsw.sj.common.module

import java.io.File
import java.net.{InetSocketAddress, URLClassLoader}
import java.util.Properties

import com.aerospike.client.Host
import com.bwsw.common.ObjectSerializer
import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.tstreams.agents.consumer.Offsets.{IOffset, Newest}
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.consumer.{ConsumerCoordinationSettings, BasicConsumer, BasicConsumerOptions}
import com.bwsw.tstreams.agents.producer.InsertionType.{BatchInsert, SingleElementInsert}
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerOptions, ProducerCoordinationSettings}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.transactions.transport.impl.TcpTransport
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.data.cassandra.{CassandraStorageFactory, CassandraStorageOptions}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.BasicStream
import com.bwsw.sj.common.ModuleConstants._
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import com.bwsw.sj.common.DAL.ConnectionConstants._
import org.slf4j.LoggerFactory
import scala.collection.mutable

/**
 * Class allowing to manage an environment of task
 * Created: 13/04/2016
 *
 * @author Kseniya Mikhaleva
 */

class TaskManager() {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val moduleType = System.getenv("MODULE_TYPE")
  private val moduleName = System.getenv("MODULE_NAME")
  private val moduleVersion = System.getenv("MODULE_VERSION")
  private val instanceName = System.getenv("INSTANCE_NAME")
  val taskName = System.getenv("TASK_NAME")
  var kafkaOffsetsStorage = mutable.Map[(String, Int), Long]()
  private val kafkaOffsetsStream = taskName + "_kafka_offsets"
  private val stateStream = taskName + "_state"
  private var tempCounter = 8000
  private val instanceMetadata = ConnectionRepository.getInstanceService.get(instanceName)
  private val storage = ConnectionRepository.getFileStorage

  private val fileMetadata: FileMetadata = ConnectionRepository.getFileMetadataService.getByParameters(Map("specification.name" -> moduleName,
    "specification.module-type" -> moduleType,
    "specification.version" -> moduleVersion)).head

  /**
   * Converter to convert usertype->storagetype; storagetype->usertype
   */
  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. Converter is invoked\n")
      obj
    }
  }

  /**
   * An auxiliary service to retrieve settings of TStream providers
   */
  private val service = ConnectionRepository.getStreamService.get(instanceMetadata.outputs.head).service.asInstanceOf[TStreamService]

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
  def getClassLoader(pathToJar: String) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get class loader for class: $pathToJar\n")
    val classLoaderUrls = Array(new File(pathToJar).toURI.toURL)

    new URLClassLoader(classLoaderUrls)

  }

  /**
   * Returns file contains uploaded module jar
   * @return Local file contains uploaded module jar
   */
  def getModuleJar: File = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get file contains uploaded '$moduleName' module jar\n")
    storage.get(fileMetadata.filename, s"tmp/$moduleName")
  }

  /**
   * Returns instance metadata to launch a module
   * @return An instance metadata to launch a module
   */
  def getInstanceMetadata = {
    logger.info(s"Instance name: $instanceName, task name: $taskName. Get instance metadata\n")
    instanceMetadata
  }

  /**
   * Returns an absolute path to executor class of module
   * @return An absolute path to executor class of module
   */
  def getExecutorClass = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get an absolute path to executor class of module\n")
    fileMetadata.specification.executorClass
  }

  /**
   * Returns tags for each output stream
   * @return
   */
  def getOutputTags = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get tags for each output stream\n")
    mutable.Map[String, (String, Any)]()
  }

  /**
   * Creates a kafka consumer for all input streams of kafka type.
   * If there was a checkpoint with offsets of last consumed messages for each topic/partition
   * then consumer will fetch from this offsets otherwise in accordance with offset parameter
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

    val coordinatorSettings = new ConsumerCoordinationSettings(
      "localhost:" + tempCounter.toString,
      service.lockNamespace,
      zkHosts,
      7000
    )
    tempCounter +=1 //todo адрес консумера

    val props = new Properties()
    props.put("bootstrap.servers", hosts.mkString(","))
    props.put("enable.auto.commit", "false")
    props.put("session.timeout.ms", "30000")
    props.put("auto.offset.reset", offset)
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)

    val topicPartitions = topics.flatMap(x => {
      x._2.map(y => new TopicPartition(x._1, y))
    }).asJava

    consumer.assign(topicPartitions)

    if (BasicStreamService.isExist(kafkaOffsetsStream, metadataStorage)) {
      val stream = BasicStreamService.loadStream(kafkaOffsetsStream, metadataStorage, dataStorage)
      val roundRobinPolicy = new RoundRobinPolicy(stream, (0 to 0).toList)
      val timeUuidGenerator = new LocalTimeUUIDGenerator
      val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
        transactionsPreload = 10,
        dataPreload = 7,
        consumerKeepAliveInterval = 5,
        converter,
        roundRobinPolicy,
        coordinatorSettings,
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
   * Returns T-stream producer responsible for committing the offsets of last messages
   * that has successfully processed for each topic for each partition
   * @return T-stream producer responsible for committing the offsets of last messages
   *         that has successfully processed for each topic for each partition
   */
  def createOffsetProducer() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create t-stream producer to write kafka offsets\n")
    var stream: BasicStream[Array[Byte]] = null
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    val coordinatorSettings = new ProducerCoordinationSettings(
      agentAddress = s"localhost:" + tempCounter.toString,
      zkHosts,
      service.lockNamespace,
      zkTimeout = 7000,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport,
      transportTimeout = 5)
    tempCounter += 1 //todo

    if (BasicStreamService.isExist(kafkaOffsetsStream, metadataStorage)) {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. Load t-stream: $kafkaOffsetsStream for kafka offsets\n")
      stream = BasicStreamService.loadStream(kafkaOffsetsStream, metadataStorage, dataStorage)
    } else {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. Create t-stream: $kafkaOffsetsStream for kafka offsets\n")
      stream = BasicStreamService.createStream(
        kafkaOffsetsStream,
        1,
        1000 * 60,
        "stream to store kafka offsets of input streams",
        metadataStorage,
        dataStorage
      )
    }

    val options = new BasicProducerOptions[Array[Byte], Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      producerKeepAliveInterval = 1,
      new RoundRobinPolicy(stream, (0 to 0).toList),
      SingleElementInsert,
      new LocalTimeUUIDGenerator,
      coordinatorSettings,
      converter)

    new BasicProducer[Array[Byte], Array[Byte]](stream.name, stream, options)
  }

  /**
   * Creates a t-stream consumer with pub/sub property 
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

    val coordinatorSettings = new ConsumerCoordinationSettings(
      "localhost:" + tempCounter.toString,
      service.lockNamespace,
      zkHosts,
      7000
    )
    tempCounter += 1 //todo адрес консумера

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (partitions.head to partitions.tail.head).toList)

    val timeUuidGenerator =
      stream.generator.generatorType match {
        case "local" => new LocalTimeUUIDGenerator
        case _type =>
          val service = stream.generator.service.asInstanceOf[ZKService]
          val zkHosts = service.provider.hosts
          val prefix = service.namespace + "/" + {
            if (_type == "global") _type else stream.name
          }

          new NetworkTimeUUIDGenerator(zkHosts, prefix, retryInterval, retryCount)
      }

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      coordinatorSettings,
      offset,
      timeUuidGenerator,

      useLastOffset = true)

    val callback = new QueueConsumerCallback[Array[Byte], Array[Byte]](queue)

    new BasicSubscribingConsumer[Array[Byte], Array[Byte]](
      "consumer for " + taskName + "_" + stream.name,
      basicStream,
      options,
      callback,
      persistentQueuePath
    )
  }

  /**
   * Creates an ordinary t-stream consumer
   * @param stream SjStream from which massages are consumed
   * @param partitions Range of stream partition
   * @param offset Offset policy that describes where a consumer starts
   * @return Basic t-stream consumer
   */
  def createConsumer(stream: SjStream, partitions: List[Int], offset: IOffset): BasicConsumer[Array[Byte], Array[Byte]] = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create basic consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head})\n")
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    val coordinatorSettings = new ConsumerCoordinationSettings(
      "localhost:" + tempCounter.toString,
      service.lockNamespace,
      zkHosts,
      7000
    )
    tempCounter += 1 //todo

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (partitions.head to partitions.tail.head).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      coordinatorSettings,
      offset,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicConsumer[Array[Byte], Array[Byte]](
      "consumer for " + taskName + "_" + stream.name,
      basicStream,
      options
    )
  }

  /**
   * Creates a t-stream producer for recording messages
   * @param stream SjStream to which messages are written
   * @return Basic t-stream producer
   */
  def createProducer(stream: SjStream) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create basic producer for stream: ${stream.name}\n")
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    val coordinatorSettings = new ProducerCoordinationSettings(
      agentAddress = s"localhost:" + tempCounter.toString,
      zkHosts,
      service.lockNamespace,
      zkTimeout = 7000,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport,
      transportTimeout = 5)
    tempCounter += 1 //todo

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (0 until stream.partitions).toList)

    val timeUuidGenerator =
      stream.generator.generatorType match {
        case "local" => new LocalTimeUUIDGenerator
        case _type =>
          val service = stream.generator.service.asInstanceOf[ZKService]
          val zkServers = service.provider.hosts
          val prefix = service.namespace + "/" + {
            if (_type == "global") _type else basicStream.name
          }

          new NetworkTimeUUIDGenerator(zkServers, prefix, retryInterval, retryCount)
      }

    val options = new BasicProducerOptions[Array[Byte], Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      producerKeepAliveInterval = 1,
      roundRobinPolicy,
      BatchInsert(5),
      timeUuidGenerator,
      coordinatorSettings,
      converter)

    new BasicProducer[Array[Byte], Array[Byte]](
      "producer for " + taskName + "_" + stream.name,
      basicStream,
      options
    )
  }


  /**
   * Creates t-stream to keep a module state or loads an existing t-stream
   * @return SjStream used for keeping a module state
   */
  def getStateStream = {
    var stream: BasicStream[Array[Byte]] = null
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    if (BasicStreamService.isExist(stateStream, metadataStorage)) {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
        s"Load t-stream: $stateStream to store state of module\n")
      stream = BasicStreamService.loadStream(stateStream, metadataStorage, dataStorage)
    } else {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
        s"Create t-stream: $stateStream to store state of module\n")
      stream = BasicStreamService.createStream(
        stateStream,
        1,
        1000 * 60,
        "stream to store state of module",
        metadataStorage,
        dataStorage
      )
    }

    new SjStream(stream.getName, stream.getDescriptions, stream.getPartitions, new Generator("local"))
  }
}
