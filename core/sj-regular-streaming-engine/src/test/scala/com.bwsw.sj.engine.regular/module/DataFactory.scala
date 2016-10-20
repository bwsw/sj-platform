package com.bwsw.sj.engine.regular.module

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.Properties
import java.util.jar.JarFile

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.{Instance, RegularInstance, Task}
import com.bwsw.sj.common.DAL.model.{Provider, Service, _}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.module.ExecutionPlan
import com.bwsw.sj.common.utils.{GeneratorLiterals, _}
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}
import com.bwsw.tstreams.generator.LocalTransactionGenerator
import com.bwsw.tstreams.services.BasicStreamService
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
  private val cassandraHost = System.getenv("CASSANDRA_HOST")
  private val cassandraPort = System.getenv("CASSANDRA_PORT").toInt
  private val agentsHost = "localhost"
  private val cassandraTestKeyspace = "test_keyspace_for_regular_engine"
  private val testNamespace = "test"
  private val instanceName = "test-instance-for-regular-engine"
  private var instanceInputs: Array[String] = Array()
  private var instanceOutputs: Array[String] = Array()
  private val task: Task = new Task()
  private val serializer = new JsonSerializer()
  private val objectSerializer = new ObjectSerializer()
  private val cassandraFactory = new CassandraFactory()
  private val cassandraProvider = new Provider("cassandra-test-provider", "cassandra provider", Array(s"$cassandraHost:$cassandraPort"), "", "", ProviderLiterals.cassandraType)
  private val zookeeperProvider = new Provider("zookeeper-test-provider", "zookeeper provider", zookeeperHosts, "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamService("tstream-test-service", ServiceLiterals.tstreamsType, "tstream test service",
    cassandraProvider, cassandraTestKeyspace, cassandraProvider, cassandraTestKeyspace, zookeeperProvider, "unit")
  private val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()

  val inputCount = 2
  val outputCount = 2
  val partitions = 4

  private def setTStreamFactoryProperties() = {
    setMetadataClusterProperties(tstrqService)
    setDataClusterProperties(tstrqService)
    setCoordinationOptions(tstrqService)
    setBindHostForAgents()
  }

  private def setMetadataClusterProperties(tStreamService: TStreamService) = {
    tstreamFactory.setProperty(TSF_Dictionary.Metadata.Cluster.NAMESPACE, tStreamService.metadataNamespace)
      .setProperty(TSF_Dictionary.Metadata.Cluster.ENDPOINTS, tStreamService.metadataProvider.hosts.mkString(","))
  }

  private def setDataClusterProperties(tStreamService: TStreamService) = {
    tStreamService.dataProvider.providerType match {
      case ProviderLiterals.aerospikeType =>
        tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.DRIVER, TSF_Dictionary.Data.Cluster.Consts.DATA_DRIVER_AEROSPIKE)
      case _ =>
        tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.DRIVER, TSF_Dictionary.Data.Cluster.Consts.DATA_DRIVER_CASSANDRA)
    }

    tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.NAMESPACE, tStreamService.dataNamespace)
      .setProperty(TSF_Dictionary.Data.Cluster.ENDPOINTS, tStreamService.dataProvider.hosts.mkString(","))
  }

  private def setCoordinationOptions(tStreamService: TStreamService) = {
    tstreamFactory.setProperty(TSF_Dictionary.Coordination.ROOT, s"/${tStreamService.lockNamespace}")
      .setProperty(TSF_Dictionary.Coordination.ENDPOINTS, tStreamService.lockProvider.hosts.mkString(","))
  }

  private def setBindHostForAgents() = {
    tstreamFactory.setProperty(TSF_Dictionary.Producer.BIND_HOST, agentsHost)
    tstreamFactory.setProperty(TSF_Dictionary.Consumer.Subscriber.BIND_HOST, agentsHost)
  }

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  //  private val hosts = aerospikeHosts.map(x => {
  //    val hostPort = x.split(":")
  //    new Host(hostPort(0), hostPort(1).toInt)
  //  }).toList
  //  private lazy val aerospikeOptions = new AerospikeStorageOptions(testNamespace, hosts)
  //  private lazy val aerospikeStorageFactory = new AerospikeStorageFactory()


  def open() = cassandraFactory.open(Set((cassandraHost, cassandraPort)))

  def cassandraSetup() = {
    cassandraFactory.createKeyspace(cassandraTestKeyspace)
    cassandraFactory.createMetadataTables(cassandraTestKeyspace)
    cassandraFactory.createDataTable(cassandraTestKeyspace)
  }

  def cassandraDestroy() = {
    cassandraFactory.dropKeyspace(cassandraTestKeyspace)
  }

  def close() = cassandraFactory.close()

  def createProviders(providerService: GenericMongoService[Provider]) = {
    providerService.save(cassandraProvider)

    //    val aerospikeProvider = new Provider("aerospike-test-provider", "aerospike provider", aerospikeHosts, "", "", Provider.aerospikeType)
    //    providerService.save(aerospikeProvider)

    val kafkaProvider = new Provider("kafka-test-provider", "kafka provider", kafkaHosts, "", "", ProviderLiterals.kafkaType)
    providerService.save(kafkaProvider)

    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete("cassandra-test-provider")
    //providerService.delete("aerospike-test-provider")
    providerService.delete("kafka-test-provider")
    providerService.delete("zookeeper-test-provider")
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val cassService = new CassandraService("cassandra-test-service", ServiceLiterals.cassandraType, "cassandra test service", cassandraProvider, cassandraTestKeyspace)
    serviceManager.save(cassService)

    //    val aeroProv = providerService.get("aerospike-test-provider")
    //    val aeroService = new AerospikeService("aerospike-test-service", "ArspkDB", "aerospike test service", aeroProv, testNamespace)
    //    serviceManager.save(aeroService)

    val zkService = new ZKService("zookeeper-test-service", ServiceLiterals.zookeeperType, "zookeeper test service", zookeeperProvider, testNamespace)
    serviceManager.save(zkService)

    val kafkaProv = providerService.get("kafka-test-provider").get
    val kafkaService = new KafkaService("kafka-test-service", ServiceLiterals.kafkaType, "kafka test service", kafkaProv, zookeeperProvider, testNamespace)
    serviceManager.save(kafkaService)

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
    val localGenerator = new Generator(GeneratorLiterals.localType)

    val tService = serviceManager.get("tstream-test-service").get

    val s1 = new TStreamSjStream("test-input-tstream" + suffix, "test-input-tstream", partitions, tService, StreamLiterals.tStreamType, Array("input"), localGenerator)
    sjStreamService.save(s1)

    val metadataStorage = cassandraFactory.getMetadataStorage(cassandraTestKeyspace)
    val dataStorage = cassandraFactory.getDataStorage(cassandraTestKeyspace)

    BasicStreamService.createStream(
      "test-input-tstream" + suffix,
      partitions,
      1000 * 60,
      "description of test input tstream",
      metadataStorage,
      dataStorage
    )
  }

  private def createOutputTStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val localGenerator = new Generator(GeneratorLiterals.localType)

    val tService = serviceManager.get("tstream-test-service").get

    val s2 = new TStreamSjStream("test-output-tstream" + suffix, "test-output-tstream", partitions, tService, StreamLiterals.tStreamType, Array("output", "some tags"), localGenerator)
    sjStreamService.save(s2)

    val metadataStorage = cassandraFactory.getMetadataStorage(cassandraTestKeyspace)
    val dataStorage = cassandraFactory.getDataStorage(cassandraTestKeyspace)

    BasicStreamService.createStream(
      "test-output-tstream" + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream",
      metadataStorage,
      dataStorage
    )
  }

  private def deleteInputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete("test-input-tstream" + suffix)
    val metadataStorage = cassandraFactory.getMetadataStorage(cassandraTestKeyspace)

    BasicStreamService.deleteStream("test-input-tstream" + suffix, metadataStorage)
  }

  private def deleteOutputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete("test-output-tstream" + suffix)
    val metadataStorage = cassandraFactory.getMetadataStorage(cassandraTestKeyspace)

    BasicStreamService.deleteStream("test-output-tstream" + suffix, metadataStorage)
  }

  private def createKafkaStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int) = {
    val kService = serviceManager.get("kafka-test-service").get.asInstanceOf[KafkaService]
    val replicationFactor = 1

    val s1 = new KafkaSjStream("kafka-input1", "kafka-input1", partitions, kService, StreamLiterals.kafkaStreamType, Array("kafka input"), replicationFactor)
    sjStreamService.save(s1)

    val s2 = new KafkaSjStream("kafka-input2", "kafka-input2", partitions, kService, StreamLiterals.kafkaStreamType, Array("kafka input"), replicationFactor)
    sjStreamService.save(s2)

    try {
      val replications = replicationFactor
      val zkHost = kService.zkProvider.hosts
      val zkConnect = new ZkConnection(zkHost.mkString(";"))
      val zkTimeout = ConnectionRepository.getConfigService.get(ConfigLiterals.zkSessionTimeoutTag).get.value.toInt
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
                     stateManagement: String = EngineLiterals.noneStateMode,
                     stateFullCheckpoint: Int = 0
                      ) = {
    import scala.collection.JavaConverters._

    val instance = new RegularInstance()
    instance.name = instanceName
    instance.moduleType = EngineLiterals.regularStreamingType
    instance.moduleName = "regular-streaming-stub"
    instance.moduleVersion = "1.0"
    instance.status = EngineLiterals.started
    instance.description = "some description of test instance"
    instance.inputs = instanceInputs
    instance.outputs = instanceOutputs
    instance.checkpointMode = EngineLiterals.everyNthMode
    instance.checkpointInterval = checkpointInterval
    instance.stateManagement = stateManagement
    instance.stateFullCheckpoint = stateFullCheckpoint
    instance.parallelism = 1
    instance.options = """{"hey": "hey"}"""
    instance.startFrom = EngineLiterals.oldestStartMode
    instance.perTaskCores = 0.1
    instance.perTaskRam = 64
    instance.performanceReportingInterval = 10000
    instance.executionPlan = new ExecutionPlan(Map((instanceName + "-task0", task), (instanceName + "-task1", task)).asJava)
    instance.engine = "com.bwsw.regular.streaming.engine-1.0"
    instance.eventWaitTime = 10
    instance.coordinationService = serviceManager.get("zookeeper-test-service").get.asInstanceOf[ZKService]

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
    val policy = producer.NewTransactionProducerPolicy.ErrorIfOpened

    def createTstreamData(countTxns: Int, countElements: Int, streamService: GenericMongoService[SjStream], suffix: String) = {
      //val producer = createProducer(streamService.get("test-input-tstream" + suffix).get.asInstanceOf[TStreamSjStream])
      val producer = createProducer(streamService.get("uwindowed-stream2").get.asInstanceOf[TStreamSjStream])
      val s = System.currentTimeMillis()
      (0 until countTxns) foreach { (x: Int) =>
        val transaction = producer.newTransaction(policy)
        (0 until countElements) foreach { (y: Int) =>
          number += 1
          transaction.send(objectSerializer.serialize(number.asInstanceOf[Object]))
        }
        transaction.checkpoint()
      }

      println(s"producer time: ${(System.currentTimeMillis() - s) / 1000}")

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
    val s = System.currentTimeMillis()
    (0 until countTxns) foreach { (x: Int) =>
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        val record = new ProducerRecord[Array[Byte], Array[Byte]]("kafka-input1", Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object]))
        val record2 = new ProducerRecord[Array[Byte], Array[Byte]]("kafka-input2", Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object]))
        producer.send(record)
        producer.send(record2)
      }
    }

    println(s"producer time: ${(System.currentTimeMillis() - s) / 1000}")
    producer.close()
  }

  def createInputTstreamConsumer(streamService: GenericMongoService[SjStream], suffix: String) = {
    createConsumer("test-input-tstream" + suffix, streamService)
  }

  def createStateConsumer(streamService: GenericMongoService[SjStream]) = {
    val name = instanceName + "-task0" + "_state"
    val partitions = 1
    val transactionGenerator = new LocalTransactionGenerator

    tstreamFactory.setProperty(TSF_Dictionary.Stream.NAME, name)
    tstreamFactory.setProperty(TSF_Dictionary.Stream.PARTITIONS, partitions)

    tstreamFactory.getConsumer[Array[Byte]](
      name,
      transactionGenerator,
      converter,
      (0 until partitions).toSet,
      Oldest)
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
    createConsumer("test-output-tstream" + suffix, streamService)
  }

  def createProducer(stream: TStreamSjStream) = {
    val transactionGenerator = new LocalTransactionGenerator

    setProducerBindPort()
    setStreamOptions(stream)

    tstreamFactory.getProducer[Array[Byte]](
      "producer for " + stream.name,
      transactionGenerator,
      converter,
      (0 until stream.partitions).toSet)
  }

  private def setProducerBindPort() = {
    tstreamFactory.setProperty(TSF_Dictionary.Producer.BIND_PORT, 8030)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoService[SjStream]): Consumer[Array[Byte]] = {
    val stream = streamService.get(streamName).get.asInstanceOf[TStreamSjStream]
    val transactionGenerator = new LocalTransactionGenerator

    setStreamOptions(stream)

    tstreamFactory.getConsumer[Array[Byte]](
      streamName,
      transactionGenerator,
      converter,
      (0 until stream.partitions).toSet,
      Oldest)
  }

  protected def setStreamOptions(stream: TStreamSjStream) = {
    tstreamFactory.setProperty(TSF_Dictionary.Stream.NAME, stream.name)
    tstreamFactory.setProperty(TSF_Dictionary.Stream.PARTITIONS, stream.partitions)
    tstreamFactory.setProperty(TSF_Dictionary.Stream.DESCRIPTION, stream.description)
  }
}
