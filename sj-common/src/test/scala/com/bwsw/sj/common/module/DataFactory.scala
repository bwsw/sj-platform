package com.bwsw.sj.common.module

import java.io.{BufferedReader, File, InputStreamReader}
import java.net.InetSocketAddress
import java.util.Properties
import java.util.jar.JarFile

import com.aerospike.client.Host
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{Instance, ExecutionPlan, RegularInstance, Task}
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.utils.CassandraHelper._
import com.bwsw.tstreams.agents.consumer.Offsets.Oldest
import com.bwsw.tstreams.agents.consumer.{ConsumerCoordinationSettings, BasicConsumer, BasicConsumerOptions}
import com.bwsw.tstreams.agents.producer.InsertionType.BatchInsert
import com.bwsw.tstreams.agents.producer.{ProducerCoordinationSettings, BasicProducer, BasicProducerOptions, ProducerPolicies}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.transactions.transport.impl.TcpTransport
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.BasicStream
import com.datastax.driver.core.Cluster
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._

object DataFactory {

  private val aerospikeHosts: Array[String] = System.getenv("AEROSPIKE_HOSTS").split(",")
  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",")
  private val redisHosts = System.getenv("REDIS_HOSTS").split(",")
  private val kafkaHosts = System.getenv("KAFKA_HOSTS").split(",")
  private val testNamespace = "test"
  private val instanceName = "test-instance"
  private var instanceInputs: Array[String] = Array()
  private var instanceOutputs: Array[String] = Array()
  private val task: Task = new Task(new java.util.HashMap[String, Array[Int]]())
  private val serializer = new JsonSerializer()
  private val objectSerializer = new ObjectSerializer()
  private val cluster = Cluster.builder().addContactPoint(cassandraHost).build()
  private val session = cluster.connect()
  val inputCount = 2
  val outputCount = 2

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  private val hosts = aerospikeHosts.map(x => {
    val hostPort = x.split(":")
    new Host(hostPort(0), hostPort(1).toInt)
  }).toList
  private lazy val aerospikeOptions = new AerospikeStorageOptions(testNamespace, hosts)
  private lazy val aerospikeStorageFactory = new AerospikeStorageFactory()

  private lazy val metadataStorageFactory: MetadataStorageFactory = new MetadataStorageFactory()
  private lazy val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(
    cassandraHosts = List(new InetSocketAddress(cassandraHost, cassandraPort)),
    keyspace = cassandraTestKeyspace)

  def cassandraSetup() = {
    createKeyspace(session, cassandraTestKeyspace)
    createMetadataTables(session, cassandraTestKeyspace)
  }

  def cassandraDestroy() = {
    session.execute(s"DROP KEYSPACE $cassandraTestKeyspace")
  }

  def close() = {
    aerospikeStorageFactory.closeFactory()
    metadataStorageFactory.closeFactory()
    session.close()
    cluster.close()
  }

  def createProviders(providerService: GenericMongoService[Provider]) = {
    val cassandraProvider = new Provider("cassandra test provider", "cassandra provider", Array(s"$cassandraHost:9042"), "", "", "cassandra")
    providerService.save(cassandraProvider)

    val aerospikeProvider = new Provider("aerospike test provider", "aerospike provider", aerospikeHosts, "", "", "aerospike")
    providerService.save(aerospikeProvider)

    val redisProvider = new Provider("redis test provider", "redis provider", redisHosts, "", "", "redis")
    providerService.save(redisProvider)

    val kafkaProvider = new Provider("kafka test provider", "kafka provider", kafkaHosts, "", "", "kafka")
    providerService.save(kafkaProvider)

    val zookeeperProvider = new Provider("zookeeper test provider", "zookeeper provider", zookeeperHosts, "", "", "zookeeper")
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete("cassandra test provider")
    providerService.delete("aerospike test provider")
    providerService.delete("redis test provider")
    providerService.delete("kafka test provider")
    providerService.delete("zookeeper test provider")
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val cassProv = providerService.get("cassandra test provider")
    val cassService = new CassandraService("cassandra test service", "CassDB", "cassandra test service", cassProv, cassandraTestKeyspace)
    serviceManager.save(cassService)

    val aeroProv = providerService.get("aerospike test provider")
    val aeroService = new AerospikeService("aerospike test service", "ArspkDB", "aerospike test service", aeroProv, testNamespace)
    serviceManager.save(aeroService)

    val redisProv = providerService.get("redis test provider")
    val redisService = new RedisService("redis test service", "RdsCoord", "redis test service", redisProv, testNamespace)
    serviceManager.save(redisService)

    val kafkaProv = providerService.get("kafka test provider")
    val kafkaService = new KafkaService("kafka test service", "KfkQ", "kafka test service", kafkaProv)
    serviceManager.save(kafkaService)

    val zkProv = providerService.get("zookeeper test provider")

    val tstrqService = new TStreamService("tstream test service", "TstrQ", "tstream test service",
      cassProv, cassandraTestKeyspace, aeroProv, testNamespace, zkProv, "/unit")
    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoService[Service]) = {
    serviceManager.delete("cassandra test service")
    serviceManager.delete("aerospike test service")
    serviceManager.delete("redis test service")
    serviceManager.delete("kafka test service")
    serviceManager.delete("tstream test service")
  }

  def createStreams(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service],
                    partitions: Int, _type: String, inputCount: Int, outputCount: Int) = {
    _type match {
      case "tstream" =>
        (1 to inputCount).foreach(x => {
          createInputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"test_input_tstream$x/split"
          task.inputs.put(s"test_input_tstream$x", (0 until partitions).toArray)
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ s"test_output_tstream$x"
        })
      case "kafka" =>
        createKafkaStream(sjStreamService, serviceManager, partitions)
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ s"test_output_tstream$x"
        })
        instanceInputs = instanceInputs :+ "test_kafka_input1/split"
        task.inputs.put(s"test_kafka_input1", (0 until partitions).toArray)
        instanceInputs = instanceInputs :+ "test_kafka_input2/split"
        task.inputs.put(s"test_kafka_input2", (0 until partitions).toArray)
      case "both" =>
        (1 to inputCount).foreach(x => {
          createInputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"test_input_tstream$x/split"
          task.inputs.put(s"test_input_tstream$x", (0 until partitions).toArray)
        })
        createKafkaStream(sjStreamService, serviceManager, partitions)
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ s"test_output_tstream$x"
        })
        instanceInputs = instanceInputs :+ "test_kafka_input1/split"
        task.inputs.put(s"test_kafka_input1", (0 until partitions).toArray)
        instanceInputs = instanceInputs :+ "test_kafka_input2/split"
        task.inputs.put(s"test_kafka_input2", (0 until partitions).toArray)
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: 'tstream', 'kafka', 'both'")
    }
  }

  def deleteStreams(streamService: GenericMongoService[SjStream], _type: String, inputCount: Int, outputCount: Int) = {
    _type match {
      case "tstream" =>
        (1 to inputCount).foreach(x => deleteInputTStream(streamService, x.toString))
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case "kafka" =>
        deleteKafkaStream(streamService)
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case "both" =>
        deleteKafkaStream(streamService)
        (1 to inputCount).foreach(x => deleteInputTStream(streamService, x.toString))
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: 'tstream', 'kafka', 'both'")
    }
  }

  private def createInputTStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val localGenerator = new Generator("local")

    val tService = serviceManager.get("tstream test service")

    val s1 = new SjStream("test_input_tstream" + suffix, "test_input_tstream", partitions, tService, "Tstream", Array("input"), localGenerator)
    sjStreamService.save(s1)

    BasicStreamService.createStream(
      "test_input_tstream" + suffix,
      partitions,
      1000 * 60,
      "description of test input tstream",
      metadataStorage,
      aerospikeStorageFactory.getInstance(aerospikeOptions)
    )
  }

  private def createOutputTStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val localGenerator = new Generator("local")

    val tService = serviceManager.get("tstream test service")

    val s2 = new SjStream("test_output_tstream" + suffix, "test_output_tstream", partitions, tService, "Tstream", Array("output", "some tags"), localGenerator)
    sjStreamService.save(s2)

    BasicStreamService.createStream(
      "test_output_tstream" + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream",
      metadataStorage,
      aerospikeStorageFactory.getInstance(aerospikeOptions)
    )
  }

  private def deleteInputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete("test_input_tstream" + suffix)
    BasicStreamService.deleteStream("test_input_tstream" + suffix, metadataStorage)
  }

  private def deleteOutputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete("test_output_tstream" + suffix)
    BasicStreamService.deleteStream("test_output_tstream" + suffix, metadataStorage)
  }

  private def createKafkaStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int) = {
    val kService = serviceManager.get("kafka test service")

    val s1 = new SjStream("test_kafka_input1", "test_kafka_input1", partitions, kService, "kafka", Array("kafka input"), null)
    sjStreamService.save(s1)

    val s2 = new SjStream("test_kafka_input2", "test_kafka_input2", partitions, kService, "kafka", Array("kafka input"), null)
    sjStreamService.save(s2)
  }

  private def deleteKafkaStream(streamService: GenericMongoService[SjStream]) = {
    streamService.delete("test_kafka_input1")
    streamService.delete("test_kafka_input2")
  }


  def createInstance(instanceService: GenericMongoService[Instance],
                     checkpointInterval: Int,
                     stateManagement: String = "none",
                     stateFullCheckpoint: Int = 0
                      ) = {
    import scala.collection.JavaConverters._

    val instance = new RegularInstance()
    instance.name = instanceName
    instance.moduleType = "regular-streaming"
    instance.moduleName = "test stub"
    instance.moduleVersion = "0.1"
    instance.status = "started"
    instance.description = "some description of test instance"
    instance.inputs = instanceInputs
    instance.outputs = instanceOutputs
    instance.checkpointMode = "every-nth"
    instance.checkpointInterval = checkpointInterval
    instance.stateManagement = stateManagement
    instance.stateFullCheckpoint = stateFullCheckpoint
    instance.parallelism = 0
    instance.options = """{"hey": "hey"}"""
    instance.startFrom = "oldest"
    instance.perTaskCores = 0
    instance.perTaskRam = 0
    instance.executionPlan = new ExecutionPlan(Map((instanceName + "-task0", task)).asJava)

    instance.eventWaitTime = 10

    instanceService.save(instance)
  }

  def deleteInstance(instanceService: GenericMongoService[Instance]) = {
    instanceService.delete(instanceName)
  }

  def loadModule(file: File, storage: FileStorage) = {
    val builder = new StringBuilder
    val jar = new JarFile(file)
    val enu = jar.entries()
    while (enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        try {
          var line = reader.readLine
          while (line != null) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        } finally {
          reader.close()
        }
      }
    }

    val specification = serializer.deserialize[Map[String, Any]](builder.toString())

    storage.put(file, file.getName, specification, "module")
  }

  def deleteModule(storage: FileStorage, filename: String) = {
    storage.delete(filename)
  }

  def createData(countTxns: Int, countElements: Int, streamService: GenericMongoService[SjStream], _type: String, count: Int) = {
    _type match {
      case "tstream" =>
        (1 to count).foreach(x => createTstreamData(countTxns, countElements, streamService, x.toString))
      case "kafka" =>
      //createKafkaData(countTxns, countElements) //needed only one because of kafka doesn't allow delete topics
      case "both" =>
        (1 to count).foreach(x => createTstreamData(countTxns, countElements, streamService, x.toString))
      //createKafkaData(countTxns, countElements) //needed only one because of kafka doesn't allow delete topics
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: 'tstream', 'kafka', 'both'")
    }
  }

  private def createTstreamData(countTxns: Int, countElements: Int, streamService: GenericMongoService[SjStream], suffix: String) = {
    val producer = createProducer(streamService.get("test_input_tstream" + suffix))
    var number = 0
    val s = System.nanoTime
    (0 until countTxns) foreach { (x: Int) =>
      val txn = producer.newTransaction(ProducerPolicies.errorIfOpen)
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        txn.send(objectSerializer.serialize(number.asInstanceOf[Object]))
      }
      txn.checkpoint()
    }

    println(s"producer time: ${(System.nanoTime - s) / 1000000}")

    producer.stop()
  }

  private def createKafkaData(countTxns: Int, countElements: Int) = {
    val props = new Properties()
    props.put("bootstrap.servers", System.getenv("KAFKA_HOSTS"))
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")

    val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
    var number = 0
    val s = System.nanoTime()
    (0 until countTxns) foreach { (x: Int) =>
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        val record = new ProducerRecord[Array[Byte], Array[Byte]]("test_kafka_input2", Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object]))
        producer.send(record)
      }
    }

    println(s"producer time: ${(System.nanoTime - s) / 1000000}")
    producer.close()
  }

  def createInputTstreamConsumer(streamService: GenericMongoService[SjStream], suffix: String) = {
    createConsumer("test_input_tstream" + suffix, streamService, "localhost:804" + suffix)
  }

  def createStateConsumer(streamService: GenericMongoService[SjStream]) = {
    val name = instanceName + "-task0" + "_state"
    val partitions = 1

    val coordinationSettings = new ConsumerCoordinationSettings(
      "localhost:8050",
      "/unit",
      List(new InetSocketAddress("localhost", 2181)),
      7000
    )

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(name, metadataStorage, aerospikeStorageFactory.getInstance(aerospikeOptions))

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (0 until partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      coordinationSettings,
      Oldest,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicConsumer[Array[Byte], Array[Byte]](basicStream.name, basicStream, options)
  }

  def createInputKafkaConsumer(streamService: GenericMongoService[SjStream]) = {
    val partitions = 4

    val props = new Properties()
    props.put("bootstrap.servers", System.getenv("KAFKA_HOSTS"))
    props.put("enable.auto.commit", "false")
    props.put("session.timeout.ms", "30000")
    props.put("auto.offset.reset", "earliest")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)

    val topicPartitions = (0 until partitions).flatMap(y => {
      Array(new TopicPartition("test_kafka_input1", y), new TopicPartition("test_kafka_input2", y))
    }).asJava

    consumer.assign(topicPartitions)

    consumer
  }

  def createOutputConsumer(streamService: GenericMongoService[SjStream], suffix: String) = {
    createConsumer("test_output_tstream" + suffix, streamService, "localhost:805" + suffix)
  }

  private def createProducer(stream: SjStream) = {
    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, aerospikeStorageFactory.getInstance(aerospikeOptions))

    val coordinationSettings = new ProducerCoordinationSettings(
      agentAddress = s"localhost:8030",
      zkHosts = List(new InetSocketAddress("localhost", 2181)),
      zkRootPath = "/unit",
      zkTimeout = 7000,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport,
      transportTimeout = 5)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (0 until stream.partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new BasicProducerOptions[Array[Byte], Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      producerKeepAliveInterval = 1,
      roundRobinPolicy,
      BatchInsert(5),
      timeUuidGenerator,
      coordinationSettings,
      converter)

    new BasicProducer[Array[Byte], Array[Byte]]("producer for " + basicStream.name, basicStream, options)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoService[SjStream], address: String): BasicConsumer[Array[Byte], Array[Byte]] = {
    val stream = streamService.get(streamName)

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, aerospikeStorageFactory.getInstance(aerospikeOptions))

    val coordinationSettings = new ConsumerCoordinationSettings(
      address,
      "/unit",
      List(new InetSocketAddress("localhost", 2181)),
      7000
    )

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (0 until stream.partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      coordinationSettings,
      Oldest,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicConsumer[Array[Byte], Array[Byte]](basicStream.name, basicStream, options)
  }
}
