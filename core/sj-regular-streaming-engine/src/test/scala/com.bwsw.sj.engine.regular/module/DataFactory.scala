package com.bwsw.sj.engine.regular.module

import java.io.{BufferedReader, File, InputStreamReader}
import java.net.InetSocketAddress
import java.util.Properties
import java.util.jar.JarFile

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{ExecutionPlan, Instance, RegularInstance, Task}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.{ConfigConstants, StreamConstants}
import com.bwsw.sj.engine.core.utils.CassandraHelper._
import com.bwsw.tstreams.agents.consumer.Offsets.Oldest
import com.bwsw.tstreams.agents.consumer.{Consumer, ConsumerOptions}
import com.bwsw.tstreams.agents.producer.DataInsertType.BatchInsert
import com.bwsw.tstreams.agents.producer._
import com.bwsw.tstreams.common.CassandraConnectorConf
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.producer.transport.impl.TcpTransport
import com.bwsw.tstreams.data.cassandra.CassandraStorageFactory
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.datastax.driver.core.Cluster
import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._

object DataFactory {

  //private val aerospikeHosts: Array[String] = System.getenv("AEROSPIKE_HOSTS").split(",")
  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",")
  private val kafkaHosts = System.getenv("KAFKA_HOSTS").split(",")
  private val testNamespace = "test"
  private val instanceName = "test-instance-for-regular-engine"
  private var instanceInputs: Array[String] = Array()
  private var instanceOutputs: Array[String] = Array()
  private val task: Task = new Task(new java.util.HashMap[String, Array[Int]]())
  private val serializer = new JsonSerializer()
  private val objectSerializer = new ObjectSerializer()
  private val cluster = Cluster.builder().addContactPoint(cassandraHost).build()
  private val session = cluster.connect()
  private val dataStorageFactory = new CassandraStorageFactory()
  private val cassandraConnectorConf = CassandraConnectorConf.apply(Set(new InetSocketAddress(cassandraHost, cassandraPort)))

  val inputCount = 2
  val outputCount = 2
  val partitions = 4

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  //  private val hosts = aerospikeHosts.map(x => {
  //    val hostPort = x.split(":")
  //    new Host(hostPort(0), hostPort(1).toInt)
  //  }).toList
  //  private lazy val aerospikeOptions = new AerospikeStorageOptions(testNamespace, hosts)
  //  private lazy val aerospikeStorageFactory = new AerospikeStorageFactory()

  private lazy val metadataStorageFactory: MetadataStorageFactory = new MetadataStorageFactory()
  private lazy val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(
    cassandraConnectorConf,
    keyspace = cassandraTestKeyspace)

  def cassandraSetup() = {
    createKeyspace(session, cassandraTestKeyspace)
    createMetadataTables(session, cassandraTestKeyspace)
  }

  def cassandraDestroy() = {
    session.execute(s"DROP KEYSPACE $cassandraTestKeyspace")
  }

  def close() = {
    metadataStorageFactory.closeFactory()
    session.close()
    cluster.close()
  }

  def createProviders(providerService: GenericMongoService[Provider]) = {
    val cassandraProvider = new Provider("cassandra-test-provider", "cassandra provider", Array(s"$cassandraHost:9042"), "", "", "cassandra")
    providerService.save(cassandraProvider)

    //    val aerospikeProvider = new Provider("aerospike-test-provider", "aerospike provider", aerospikeHosts, "", "", "aerospike")
    //    providerService.save(aerospikeProvider)

    val kafkaProvider = new Provider("kafka-test-provider", "kafka provider", kafkaHosts, "", "", "kafka")
    providerService.save(kafkaProvider)

    val zookeeperProvider = new Provider("zookeeper-test-provider", "zookeeper provider", zookeeperHosts, "", "", "zookeeper")
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete("cassandra-test-provider")
    //providerService.delete("aerospike-test-provider")
    providerService.delete("kafka-test-provider")
    providerService.delete("zookeeper-test-provider")
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val cassProv = providerService.get("cassandra-test-provider")
    val cassService = new CassandraService("cassandra-test-service", "CassDB", "cassandra test service", cassProv, cassandraTestKeyspace)
    serviceManager.save(cassService)

    //    val aeroProv = providerService.get("aerospike-test-provider")
    //    val aeroService = new AerospikeService("aerospike-test-service", "ArspkDB", "aerospike test service", aeroProv, testNamespace)
    //    serviceManager.save(aeroService)

    val zkProv = providerService.get("zookeeper-test-provider")
    val zkService = new ZKService("zookeeper-test-service", "ZKCoord", "zookeeper test service", zkProv, testNamespace)
    serviceManager.save(zkService)

    val kafkaProv = providerService.get("kafka-test-provider")
    val kafkaService = new KafkaService("kafka-test-service", "KfkQ", "kafka test service", kafkaProv, zkProv, testNamespace)
    serviceManager.save(kafkaService)

    val tstrqService = new TStreamService("tstream-test-service", "TstrQ", "tstream test service",
      cassProv, cassandraTestKeyspace, cassProv, cassandraTestKeyspace, zkProv, "unit")
    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoService[Service]) = {
    serviceManager.delete("cassandra-test-service")
    // serviceManager.delete("aerospike-test-service")
    serviceManager.delete("kafka-test-service")
    serviceManager.delete("zookeeper-test-service")
    serviceManager.delete("tstream-test-service")
  }

  def createStreams(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service],
                    partitions: Int, _type: String, inputCount: Int, outputCount: Int) = {
    _type match {
      case "tstream" =>
        (1 to inputCount).foreach(x => {
          createInputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"test-input-tstream$x/split"
          task.inputs.put(s"test-input-tstream$x", Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ s"test-output-tstream$x"
        })
      case "kafka" =>
        createKafkaStream(sjStreamService, serviceManager, partitions)
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ s"test-output-tstream$x"
        })
        instanceInputs = instanceInputs :+ "kafka-input1/split"
        task.inputs.put(s"kafka-input1", Array(0, if (partitions > 1) partitions - 1 else 0))
        instanceInputs = instanceInputs :+ "kafka-input2/split"
        task.inputs.put(s"kafka-input2", Array(0, if (partitions > 1) partitions - 1 else 0))
      case "both" =>
        (1 to inputCount).foreach(x => {
          createInputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"test-input-tstream$x/split"
          task.inputs.put(s"test-input-tstream$x", Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        createKafkaStream(sjStreamService, serviceManager, partitions)
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ s"test-output-tstream$x"
        })
        instanceInputs = instanceInputs :+ "kafka-input1/split"
        task.inputs.put(s"kafka-input1", Array(0, if (partitions > 1) partitions - 1 else 0))
        instanceInputs = instanceInputs :+ "kafka-input2/split"
        task.inputs.put(s"kafka-input2", Array(0, if (partitions > 1) partitions - 1 else 0))
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

    val tService = serviceManager.get("tstream-test-service")

    val s1 = new TStreamSjStream("test-input-tstream" + suffix, "test-input-tstream", partitions, tService, StreamConstants.tStreamType, Array("input"), localGenerator)
    sjStreamService.save(s1)

    BasicStreamService.createStream(
      "test-input-tstream" + suffix,
      partitions,
      1000 * 60,
      "description of test input tstream",
      metadataStorage,
      dataStorageFactory.getInstance(
        cassandraConnectorConf,
        keyspace = cassandraTestKeyspace)
    )
  }

  private def createOutputTStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val localGenerator = new Generator("local")

    val tService = serviceManager.get("tstream-test-service")

    val s2 = new TStreamSjStream("test-output-tstream" + suffix, "test-output-tstream", partitions, tService, StreamConstants.tStreamType, Array("output", "some tags"), localGenerator)
    sjStreamService.save(s2)

    BasicStreamService.createStream(
      "test-output-tstream" + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream",
      metadataStorage,
      dataStorageFactory.getInstance(
        cassandraConnectorConf,
        keyspace = cassandraTestKeyspace)
    )
  }

  private def deleteInputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete("test-input-tstream" + suffix)
    BasicStreamService.deleteStream("test-input-tstream" + suffix, metadataStorage)
  }

  private def deleteOutputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete("test-output-tstream" + suffix)
    BasicStreamService.deleteStream("test-output-tstream" + suffix, metadataStorage)
  }

  private def createKafkaStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int) = {
    val kService = serviceManager.get("kafka-test-service").asInstanceOf[KafkaService]
    val replicationFactor = 1

    val s1 = new KafkaSjStream("kafka-input1", "kafka-input1", partitions, kService, StreamConstants.kafkaStreamType, Array("kafka input"), replicationFactor)
    sjStreamService.save(s1)

    val s2 = new KafkaSjStream("kafka-input2", "kafka-input2", partitions, kService, StreamConstants.kafkaStreamType, Array("kafka input"), replicationFactor)
    sjStreamService.save(s2)

    try {
      val replications = replicationFactor
      val zkHost = kService.zkProvider.hosts
      val zkConnect = new ZkConnection(zkHost.mkString(";"))
      val zkTimeout = ConnectionRepository.getConfigService.get(ConfigConstants.zkSessionTimeoutTag).value.toInt
      val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
      val zkUtils = new ZkUtils(zkClient, zkConnect, false)

      AdminUtils.createTopic(zkUtils, s1.name, partitions, replications)
      AdminUtils.createTopic(zkUtils, s2.name, partitions, replications)
    } catch {
      case e: kafka.common.TopicExistsException =>
        println("It's normal if kafka doesn't support deleting of topics")
    }
  }

  private def deleteKafkaStream(streamService: GenericMongoService[SjStream]) = {
    streamService.delete("kafka-input1")
    streamService.delete("kafka-input2")
  }


  def createInstance(serviceManager: GenericMongoService[Service],
                     instanceService: GenericMongoService[Instance],
                     checkpointInterval: Int,
                     stateManagement: String = "none",
                     stateFullCheckpoint: Int = 0
                      ) = {
    import scala.collection.JavaConverters._

    val instance = new RegularInstance()
    instance.name = instanceName
    instance.moduleType = "regular-streaming"
    instance.moduleName = "regular-streaming-stub"
    instance.moduleVersion = "1.0"
    instance.status = "started"
    instance.description = "some description of test instance"
    instance.inputs = instanceInputs
    instance.outputs = instanceOutputs
    instance.checkpointMode = "every-nth"
    instance.checkpointInterval = checkpointInterval
    instance.stateManagement = stateManagement
    instance.stateFullCheckpoint = stateFullCheckpoint
    instance.parallelism = 1
    instance.options = """{"hey": "hey"}"""
    instance.startFrom = "oldest"
    instance.perTaskCores = 0.1
    instance.perTaskRam = 64
    instance.performanceReportingInterval = 10000
    instance.executionPlan = new ExecutionPlan(Map((instanceName + "-task0", task), (instanceName + "-task1", task)).asJava)
    instance.engine = "com.bwsw.regular.streaming.engine-1.0"
    instance.eventWaitTime = 10
    instance.coordinationService = serviceManager.get("zookeeper-test-service").asInstanceOf[ZKService]

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
    var number = 0

    def createTstreamData(countTxns: Int, countElements: Int, streamService: GenericMongoService[SjStream], suffix: String) = {
      val producer = createProducer(streamService.get("test-input-tstream" + suffix))
      val s = System.nanoTime
      (0 until countTxns) foreach { (x: Int) =>
        val txn = producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
        (0 until countElements) foreach { (y: Int) =>
          number += 1
          txn.send(objectSerializer.serialize(number.asInstanceOf[Object]))
        }
        txn.checkpoint()
      }

      println(s"producer time: ${(System.nanoTime - s) / 1000000}")

      producer.stop()
    }

    _type match {
      case "tstream" =>
        (1 to count).foreach(x => createTstreamData(countTxns, countElements, streamService, x.toString))
      case "kafka" =>
      //createKafkaData(countTxns, countElements) //needed only once because of kafka doesn't allow delete topics
      case "both" =>
        (1 to count).foreach(x => createTstreamData(countTxns, countElements, streamService, x.toString))
      //createKafkaData(countTxns, countElements) //needed only one because of kafka doesn't allow delete topics
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: 'tstream', 'kafka', 'both'")
    }
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
        val record = new ProducerRecord[Array[Byte], Array[Byte]]("kafka-input1", Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object]))
        val record2 = new ProducerRecord[Array[Byte], Array[Byte]]("kafka-input2", Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object]))
        producer.send(record)
        producer.send(record2)
      }
    }

    println(s"producer time: ${(System.nanoTime - s) / 1000000}")
    producer.close()
  }

  def createInputTstreamConsumer(streamService: GenericMongoService[SjStream], suffix: String) = {
    createConsumer("test-input-tstream" + suffix, streamService, "localhost:804" + suffix)
  }

  def createStateConsumer(streamService: GenericMongoService[SjStream]) = {
    val name = instanceName + "-task0" + "_state"
    val partitions = 1

    val tStream =
      BasicStreamService.loadStream(name, metadataStorage, dataStorageFactory.getInstance(
        cassandraConnectorConf,
        keyspace = cassandraTestKeyspace))

    val roundRobinPolicy = new RoundRobinPolicy(tStream, (0 until partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new ConsumerOptions[Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      converter,
      roundRobinPolicy,
      Oldest,
      timeUuidGenerator,
      useLastOffset = true)

    new Consumer[Array[Byte]](tStream.name, tStream, options)
  }

  def createInputKafkaConsumer(streamService: GenericMongoService[SjStream], partitionNumber: Int) = {

    val props = new Properties()
    props.put("bootstrap.servers", System.getenv("KAFKA_HOSTS"))
    props.put("enable.auto.commit", "false")
    props.put("auto.offset.reset", "earliest")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)

    val topicPartitions = (0 until partitionNumber).flatMap(y => {
      Array(new TopicPartition("kafka-input1", y), new TopicPartition("kafka-input2", y))
    }).asJava

    consumer.assign(topicPartitions)

    consumer
  }

  def createOutputConsumer(streamService: GenericMongoService[SjStream], suffix: String) = {
    createConsumer("test-output-tstream" + suffix, streamService, "localhost:805" + suffix)
  }

  private def createProducer(stream: SjStream) = {
    val tStream =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorageFactory.getInstance(
        cassandraConnectorConf,
        keyspace = cassandraTestKeyspace))

    val coordinationSettings = new CoordinationOptions(
      agentAddress = s"localhost:8030",
      zkHosts = zookeeperHosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList,
      zkRootPath = "/unit",
      zkConnectionTimeout = 7000,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport,
      transportTimeout = 5,
      zkSessionTimeout = 7000)

    val roundRobinPolicy = new RoundRobinPolicy(tStream, (0 until stream.asInstanceOf[TStreamSjStream].partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new Options[Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      roundRobinPolicy,
      BatchInsert(5),
      timeUuidGenerator,
      coordinationSettings,
      converter)

    new Producer[Array[Byte]]("producer for " + tStream.name, tStream, options)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoService[SjStream], address: String) = {
    val stream = streamService.get(streamName)

    val tStream =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorageFactory.getInstance(
        cassandraConnectorConf,
        keyspace = cassandraTestKeyspace))

    val roundRobinPolicy = new RoundRobinPolicy(tStream, (0 until stream.asInstanceOf[TStreamSjStream].partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new ConsumerOptions[Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      converter,
      roundRobinPolicy,
      Oldest,
      timeUuidGenerator,
      useLastOffset = true)

    new Consumer[Array[Byte]](tStream.name, tStream, options)
  }
}
