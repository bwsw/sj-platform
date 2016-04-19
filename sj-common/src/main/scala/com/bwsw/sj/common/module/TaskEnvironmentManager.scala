package com.bwsw.sj.common.module

import java.io.File
import java.net.{InetSocketAddress, URLClassLoader}

import com.aerospike.client.Host
import com.bwsw.tstreams.agents.consumer.Offsets.IOffset
import com.bwsw.tstreams.agents.consumer.{BasicConsumer, BasicConsumerOptions}
import com.bwsw.tstreams.agents.producer.InsertionType.BatchInsert
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerOptions}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.Coordinator
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.generator.LocalTimeUuidGenerator
import com.bwsw.tstreams.metadata.MetadataStorageFactory
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.streams.BasicStream
import org.redisson.{Config, Redisson}

/**
 * Class allowing to manage environment of task
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

class TaskEnvironmentManager() {

  private val randomKeyspace = "test"

  //metadata/data factories
  private val metadataStorageFactory = new MetadataStorageFactory
  private val storageFactory = new AerospikeStorageFactory

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  //aerospike storage instances
  private val hosts = List(
    new Host("localhost", 3000),
    new Host("localhost", 3001),
    new Host("localhost", 3002),
    new Host("localhost", 3003))
  private val aerospikeOptions = new AerospikeStorageOptions("test", hosts)
  private val aerospikeInstForProducer = storageFactory.getInstance(aerospikeOptions)
  private val aerospikeInstForConsumer = storageFactory.getInstance(aerospikeOptions)

  //metadata storage instances
  private val metadataStorageInstForProducer = metadataStorageFactory.getInstance(
    cassandraHosts = List(new InetSocketAddress("localhost", 9042)),
    keyspace = randomKeyspace)
  private val metadataStorageInstForConsumer = metadataStorageFactory.getInstance(
    cassandraHosts = List(new InetSocketAddress("localhost", 9042)),
    keyspace = randomKeyspace)

  //coordinator for coordinating producer/consumer
  private val config = new Config()
  config.useSingleServer().setAddress("localhost:6379")
  private val redissonClient = Redisson.create(config)
  private val coordinator = new Coordinator("some_path", redissonClient)

  private val timeUuidGenerator = new LocalTimeUuidGenerator


  def getClassLoader(pathToJar: String) = {
    val classLoaderUrls = Array(new File(pathToJar).toURI.toURL)

    new URLClassLoader(classLoaderUrls)

  }

  //todo: use an Ivan REST to retrieve metadata for creating a consumer/producer

  def createConsumer(streamName: String, partitionRange: List[Int], offsetPolicy: IOffset): BasicConsumer[Array[Byte], Array[Byte]] = {

//    val stream: BasicStream[Array[Byte]] =
//      BasicStreamService.loadStream(streamName, metadataStorageInstForProducer, aerospikeInstForProducer, coordinator)

    val stream = new BasicStream[Array[Byte]](
      name = streamName,
      partitions = 20,
      metadataStorage = metadataStorageInstForConsumer,
      dataStorage = aerospikeInstForConsumer,
      coordinator = coordinator,
      ttl = 60 * 10,
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

    new BasicConsumer[Array[Byte], Array[Byte]]("consumer for " + streamName, stream, options)
  }

  def createProducer(streamName: String) = {

    //        val stream: BasicStream[Array[Byte]] =
    //          BasicStreamService.loadStream(streamName, metadataStorageInstForProducer, aerospikeInstForProducer, coordinator)

    val stream: BasicStream[Array[Byte]] = new BasicStream[Array[Byte]](
      name = streamName,
      partitions = 3,
      metadataStorage = metadataStorageInstForProducer,
      dataStorage = aerospikeInstForProducer,
      coordinator = coordinator,
      ttl = 60 * 10,
      description =  "some_description")

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
}
