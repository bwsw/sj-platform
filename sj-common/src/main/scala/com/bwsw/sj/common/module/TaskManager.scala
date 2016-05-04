package com.bwsw.sj.common.module

import java.io.File
import java.net.{InetSocketAddress, URLClassLoader}

import com.aerospike.client.Host
import com.bwsw.sj.common.DAL.model.{FileMetadata, TStreamService}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.GlobalTimeUUIDGenerator
import com.bwsw.tstreams.agents.consumer.Offsets.IOffset
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.consumer.{BasicConsumer, BasicConsumerOptions}
import com.bwsw.tstreams.agents.producer.InsertionType.BatchInsert
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerOptions}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.Coordinator
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.data.cassandra.{CassandraStorageFactory, CassandraStorageOptions}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.streams.BasicStream
import org.redisson.{Config, Redisson}

import scala.collection.mutable

/**
 * Class allowing to manage an environment of task
 * Created: 13/04/2016
 *
 * @author Kseniya Mikhaleva
 */

class TaskManager() {

  private val moduleType = System.getenv("MODULE_TYPE")
  private val moduleName = System.getenv("MODULE_NAME")
  private val moduleVersion = System.getenv("MODULE_VERSION")
  private val instanceName = System.getenv("INSTANCE_NAME")
  val taskName = System.getenv("TASK_NAME")

  private val instanceMetadata = ConnectionRepository.getInstanceService.get(instanceName)
  private val storage = ConnectionRepository.getFileStorage

  private val fileMetadata: FileMetadata = ConnectionRepository.getFileMetadataService.getByParameters(Map("specification.name" -> moduleName,
    "specification.module-type" -> moduleType,
    "specification.version" -> moduleVersion)).head

  /**
   * Converter to convert usertype->storagetype; storagetype->usertype
   */
  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  /**
   * An auxiliary stream to retrieve settings of txns generator
   */
  private val stream = ConnectionRepository.getStreamService.get(instanceMetadata.outputs.head)

  /**
   * An auxiliary service to retrieve settings of TStream providers
   */
  private val service = stream.service.asInstanceOf[TStreamService]

  /**
   * Metadata storage instance
   */
  private val metadataStorage: MetadataStorage = createMetadataStorage()

  /**
   * Data storage instance
   */
  private val dataStorage: IStorage[Array[Byte]] = createDataStorage()

  /**
   * Coordinator for coordinating producer/consumer
   */
  private val coordinator: Coordinator = createCoordinator()

  /**
   * Creates metadata storage for producer/consumer settings
   */
  private def createMetadataStorage() = {
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
        val options = new AerospikeStorageOptions(
          service.dataNamespace,
          service.dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toList)
        (new AerospikeStorageFactory).getInstance(options)

      case _ =>
        val options = new CassandraStorageOptions(
          service.dataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList,
          service.dataNamespace
        )

        (new CassandraStorageFactory).getInstance(options)
    }
  }

  /**
   * Creates coordinator for coordinating producer/consumer
   */
  private def createCoordinator() = {
    val config = new Config()
    config.useSingleServer().setAddress(service.lockProvider.hosts.head)
    val redisClient = Redisson.create(config)
    new Coordinator(service.lockNamespace, redisClient)
  }

  //todo choose from generator field and use txns generator
  private val timeUuidGenerator = stream.generator match {
    case Array(_, zkService, _) =>
      val zkServers = Array("127.0.0.1:2181")
      val prefix = "servers"
      val retryPeriod = 500
      val retryCount = 10

      new GlobalTimeUUIDGenerator(zkServers, prefix, retryPeriod, retryCount)

    case Array("local") => new LocalTimeUUIDGenerator
  }

  /**
   * Returns class loader for retrieving classes from jar
   *
   * @param pathToJar Absolute path to jar file
   * @return Class loader for retrieving classes from jar
   */
  def getClassLoader(pathToJar: String) = {
    val classLoaderUrls = Array(new File(pathToJar).toURI.toURL)

    new URLClassLoader(classLoaderUrls)

  }

  /**
   * Returns file contains uploaded module jar
   * @return Local file contains uploaded module jar
   */
  def getModuleJar: File = {
    storage.get(fileMetadata.filename, s"tmp/$moduleName")
  }

  /**
   * Returns instance metadata to launch a module
   * @return An instance metadata to launch a module
   */
  def getInstanceMetadata = {
    instanceMetadata
  }

  /**
   * Returns an absolute path to executor class of module
   * @return An absolute path to executor class of module
   */
  def getExecutorClass = {
    fileMetadata.specification.executorClass
  }

  /**
   * Returns output to temporarily store the data intended for output streams
   * @return
   */
  def getTemporaryOutput = {
    mutable.Map[String, (String, Any)]()
  }

  //todo: use BasicStreamService to retrieve metadata for retrieving streams
  def createConsumer(streamName: String, partitionRange: List[Int], offsetPolicy: IOffset, blockingQueue: PersistentBlockingQueue) = {

    //    val stream: BasicStream[Array[Byte]] =
    //      BasicStreamService.loadStream(streamName, metadataStorageInstForProducer, aerospikeInstForProducer, coordinator)

    val stream = new BasicStream[Array[Byte]](
      name = streamName,
      partitions = 3,
      metadataStorage = metadataStorage,
      dataStorage = dataStorage,
      coordinator = coordinator,
      ttl = 60 * 30,
      description = "some_description")

    val roundRobinPolicy = new RoundRobinPolicy(stream, (partitionRange.head to partitionRange.tail.head).toList)

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      offsetPolicy,
      timeUuidGenerator,
      useLastOffset = true)

    val callback = new QueueConsumerCallback[Array[Byte], Array[Byte]](blockingQueue)
    //todo
    val path = "test"

    new BasicSubscribingConsumer[Array[Byte], Array[Byte]]("consumer for " + streamName, stream, options, callback, path)
  }

  def createProducer(streamName: String) = {

    //        val stream: BasicStream[Array[Byte]] =
    //          BasicStreamService.loadStream(streamName, metadataStorageInstForProducer, aerospikeInstForProducer, coordinator)

    val stream: BasicStream[Array[Byte]] = new BasicStream[Array[Byte]](
      name = streamName,
      partitions = 3,
      metadataStorage = metadataStorage,
      dataStorage = dataStorage,
      coordinator = coordinator,
      ttl = 60 * 30,
      description = "some_description")

    val roundRobinPolicy = new RoundRobinPolicy(stream, (0 until 3).toList)

    val options = new BasicProducerOptions[Array[Byte], Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      producerKeepAliveInterval = 1,
      roundRobinPolicy,
      BatchInsert(5),
      timeUuidGenerator,
      converter)

    new BasicProducer[Array[Byte], Array[Byte]]("producer for " + streamName, stream, options)
  }

  //todo only for testing. Delete

  def createStateConsumer(streamName: String, offsetPolicy: IOffset): BasicConsumer[Array[Byte], Array[Byte]] = {

    //    val stream: BasicStream[Array[Byte]] =
    //      BasicStreamService.loadStream(streamName, metadataStorageInstForProducer, aerospikeInstForProducer, coordinator)

    val stream = new BasicStream[Array[Byte]](
      name = streamName,
      partitions = 1,
      metadataStorage = metadataStorage,
      dataStorage = dataStorage,
      coordinator = coordinator,
      ttl = 60 * 30,
      description = "some_description")

    val roundRobinPolicy = new RoundRobinPolicy(stream, (0 to 0).toList)

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      offsetPolicy,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicConsumer[Array[Byte], Array[Byte]]("consumer for " + streamName, stream, options)
  }

  def createStateProducer(streamName: String) = {

    //        val stream: BasicStream[Array[Byte]] =
    //          BasicStreamService.loadStream(streamName, metadataStorageInstForProducer, aerospikeInstForProducer, coordinator)

    val stream: BasicStream[Array[Byte]] = new BasicStream[Array[Byte]](
      name = streamName,
      partitions = 1,
      metadataStorage = metadataStorage,
      dataStorage = dataStorage,
      coordinator = coordinator,
      ttl = 60 * 30,
      description = "some_description")

    val roundRobinPolicy = new RoundRobinPolicy(stream, (0 to 0).toList)

    val options = new BasicProducerOptions[Array[Byte], Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      producerKeepAliveInterval = 1,
      roundRobinPolicy,
      BatchInsert(5),
      timeUuidGenerator,
      converter)

    new BasicProducer[Array[Byte], Array[Byte]]("producer for " + streamName, stream, options)
  }
}
