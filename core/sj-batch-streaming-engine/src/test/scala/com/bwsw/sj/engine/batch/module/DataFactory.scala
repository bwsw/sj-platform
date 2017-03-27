package com.bwsw.sj.engine.batch.module

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.Properties
import java.util.jar.JarFile

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.{Instance, Task, BatchInstance}
import com.bwsw.sj.common.DAL.model.{Provider, Service, _}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.rest.entities.module.ExecutionPlan
import com.bwsw.sj.common.utils.{GeneratorLiterals, _}
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}
import com.bwsw.tstreams.generator.LocalTransactionGenerator
import com.bwsw.tstreams.streams.StreamService
import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._

object DataFactory {

  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",")
  private val kafkaHosts = System.getenv("KAFKA_HOSTS")
  private val cassandraHost = System.getenv("CASSANDRA_HOST")
  private val cassandraPort = System.getenv("CASSANDRA_PORT").toInt
  val kafkaMode = "kafka"
  val tstreamMode = "tstream"
  val commonMode = "both"
  private val agentsHost = "localhost"
  private val cassandraTestKeyspace = "test_keyspace_for_batch_engine"
  private val testNamespace = "test_namespace_for_batch_engine"
  private val instanceName = "test-instance-for-batch-engine"
  private val tstreamInputNamePrefix = "batch-tstream-input"
  private val tstreamOutputNamePrefix = "batch-tstream-output"
  private val kafkaInputNamePrefix = "batch-kafka-input"
  private val kafkaProviderName = "batch-kafka-test-provider"
  private val cassandraProviderName = "batch-cassandra-test-provider"
  private val zookeeperProviderName = "batch-zookeeper-test-provider"
  private val tstreamServiceName = "batch-tstream-test-service"
  private val cassandraServiceName = "batch-cassandra-test-service"
  private val kafkaServiceName = "batch-kafka-test-service"
  private val zookeeperServiceName = "batch-zookeeper-test-service"
  private val replicationFactor = 1
  private var instanceInputs: Array[String] = Array()
  private var instanceOutputs: Array[String] = Array()
  private val task: Task = new Task()
  private val serializer = new JsonSerializer()
  private val objectSerializer = new ObjectSerializer()
  private val cassandraFactory = new CassandraFactory()
  private val cassandraProvider = new Provider(cassandraProviderName, cassandraProviderName, Array(s"$cassandraHost:$cassandraPort"), "", "", ProviderLiterals.cassandraType)
  private val zookeeperProvider = new Provider(zookeeperProviderName, zookeeperProviderName, zookeeperHosts, "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamService(tstreamServiceName, ServiceLiterals.tstreamsType, tstreamServiceName,
    cassandraProvider, cassandraTestKeyspace, cassandraProvider, cassandraTestKeyspace, zookeeperProvider, "batch_engine")
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

    val kafkaProvider = new Provider(kafkaProviderName, kafkaProviderName, kafkaHosts.split(","), "", "", ProviderLiterals.kafkaType)
    providerService.save(kafkaProvider)

    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete(cassandraProviderName)
    providerService.delete(kafkaProviderName)
    providerService.delete(zookeeperProviderName)
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val cassService = new CassandraService(cassandraServiceName, ServiceLiterals.cassandraType, cassandraServiceName, cassandraProvider, cassandraTestKeyspace)
    serviceManager.save(cassService)

    val zkService = new ZKService(zookeeperServiceName, ServiceLiterals.zookeeperType, zookeeperServiceName, zookeeperProvider, testNamespace)
    serviceManager.save(zkService)

    val kafkaProv = providerService.get(kafkaProviderName).get
    val kafkaService = new KafkaService(kafkaServiceName, ServiceLiterals.kafkaType, kafkaServiceName, kafkaProv, zookeeperProvider, testNamespace)
    serviceManager.save(kafkaService)

    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoService[Service]) = {
    serviceManager.delete(cassandraServiceName)
    serviceManager.delete(kafkaServiceName)
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(tstreamServiceName)
  }

  def createStreams(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service],
                    partitions: Int, _type: String, inputCount: Int, outputCount: Int) = {
    require(partitions >= 1, "Partitions must be a positive integer")
    _type match {
      case `tstreamMode` =>
        (1 to inputCount).foreach(x => {
          createInputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$tstreamInputNamePrefix$x/split"
          task.inputs.put(tstreamInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case `kafkaMode` =>
        (1 to inputCount).foreach(x => {
          createKafkaStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$kafkaInputNamePrefix$x/split"
          task.inputs.put(kafkaInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case `commonMode` =>
        (1 to inputCount).foreach(x => {
          createInputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$tstreamInputNamePrefix$x/split"
          task.inputs.put(tstreamInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))

          createKafkaStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$kafkaInputNamePrefix$x/split"
          task.inputs.put(kafkaInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: $tstreamMode, $kafkaMode, $commonMode")
    }
  }

  def deleteStreams(streamService: GenericMongoService[SjStream],
                    _type: String,
                    serviceManager: GenericMongoService[Service],
                    inputCount: Int,
                    outputCount: Int) = {
    _type match {
      case `tstreamMode` =>
        (1 to inputCount).foreach(x => deleteInputTStream(streamService, x.toString))
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case `kafkaMode` =>
        (1 to inputCount).foreach(x => deleteKafkaStream(streamService, serviceManager, x.toString))
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case `commonMode` =>
        (1 to inputCount).foreach(x => {
          deleteKafkaStream(streamService, serviceManager, x.toString)
          deleteInputTStream(streamService, x.toString)
        })
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: $tstreamMode, $kafkaMode, $commonMode")
    }
  }

  private def createInputTStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val localGenerator = new Generator(GeneratorLiterals.localType)

    val tService = serviceManager.get(tstreamServiceName).get

    val s1 = new TStreamSjStream(tstreamInputNamePrefix + suffix,
      tstreamInputNamePrefix + suffix,
      partitions, tService,
      StreamLiterals.tstreamType,
      Array("input"),
      localGenerator)

    sjStreamService.save(s1)

    val metadataStorage = cassandraFactory.getMetadataStorage(cassandraTestKeyspace)
    val dataStorage = cassandraFactory.getDataStorage(cassandraTestKeyspace)

    StreamService.createStream(
      tstreamInputNamePrefix + suffix,
      partitions,
      10 * 60,
      tstreamOutputNamePrefix + suffix,
      metadataStorage,
      dataStorage
    )
  }

  private def createOutputTStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val localGenerator = new Generator(GeneratorLiterals.localType)

    val tService = serviceManager.get(tstreamServiceName).get

    val s2 = new TStreamSjStream(tstreamOutputNamePrefix + suffix,
      tstreamOutputNamePrefix + suffix,
      partitions,
      tService,
      StreamLiterals.tstreamType,
      Array("output", "some tags"),
      localGenerator)

    sjStreamService.save(s2)

    val metadataStorage = cassandraFactory.getMetadataStorage(cassandraTestKeyspace)
    val dataStorage = cassandraFactory.getDataStorage(cassandraTestKeyspace)

    StreamService.createStream(
      tstreamOutputNamePrefix + suffix,
      partitions,
      10 * 60,
      tstreamOutputNamePrefix + suffix,
      metadataStorage,
      dataStorage
    )
  }

  private def deleteInputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete(tstreamInputNamePrefix + suffix)
    val metadataStorage = cassandraFactory.getMetadataStorage(cassandraTestKeyspace)

    StreamService.deleteStream(tstreamInputNamePrefix + suffix, metadataStorage)
  }

  private def deleteOutputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete(tstreamOutputNamePrefix + suffix)
    val metadataStorage = cassandraFactory.getMetadataStorage(cassandraTestKeyspace)

    StreamService.deleteStream(tstreamOutputNamePrefix + suffix, metadataStorage)
  }

  private def createKafkaStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val kService = serviceManager.get(kafkaServiceName).get.asInstanceOf[KafkaService]

    val kafkaSjStream = new KafkaSjStream(kafkaInputNamePrefix + suffix,
      kafkaInputNamePrefix + suffix,
      partitions,
      kService,
      StreamLiterals.kafkaStreamType,
      Array(kafkaInputNamePrefix),
      replicationFactor)

    sjStreamService.save(kafkaSjStream)

    val zkHost = kService.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConnectionRepository.getConfigService.get(ConfigLiterals.zkSessionTimeoutTag).get.value.toInt
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    AdminUtils.createTopic(zkUtils, kafkaSjStream.name, partitions, replicationFactor)
  }

  private def deleteKafkaStream(streamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], suffix: String) = {
    val kService = serviceManager.get(kafkaServiceName).get.asInstanceOf[KafkaService]
    val zkHost = kService.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConnectionRepository.getConfigService.get(ConfigLiterals.zkSessionTimeoutTag).get.value.toInt
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    AdminUtils.deleteTopic(zkUtils, kafkaInputNamePrefix + suffix)
    streamService.delete(kafkaInputNamePrefix + suffix)
  }


  def createInstance(serviceManager: GenericMongoService[Service],
                     instanceService: GenericMongoService[Instance],
                     window: Int,
                     slidingInterval: Int,
                     stateManagement: String = EngineLiterals.noneStateMode,
                     stateFullCheckpoint: Int = 0) = {
    import scala.collection.JavaConverters._

    val instance = new BatchInstance()
    instance.name = instanceName
    instance.status = EngineLiterals.started
    instance.moduleType = EngineLiterals.batchStreamingType
    instance.moduleName = "batch-streaming-stub"
    instance.moduleVersion = "1.0"
    instance.inputs = instanceInputs
    instance.window = window
    instance.slidingInterval = slidingInterval
    instance.outputs = instanceOutputs
    instance.stateManagement = stateManagement
    instance.stateFullCheckpoint = stateFullCheckpoint
    instance.startFrom = EngineLiterals.oldestStartMode
    //instance.executionPlan = new ExecutionPlan(Map((instanceName + "-task0", task), (instanceName + "-task1", task)).asJava) //for barriers test
    instance.executionPlan = new ExecutionPlan(Map(instanceName + "-task0" -> task).asJava)
    instance.engine = "com.bwsw.batch.streaming.engine-1.0"
    instance.coordinationService = serviceManager.get(zookeeperServiceName).get.asInstanceOf[ZKService]

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

    def createTstreamData(countTxns: Int, countElements: Int, suffix: String) = {
      val producer = createProducer(tstreamInputNamePrefix + suffix, partitions)
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

    def createKafkaData(countTxns: Int, countElements: Int, suffix: String) = {
      val props = new Properties()
      props.put("bootstrap.servers", kafkaHosts)
      props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")

      val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
      var number = 0
      val s = System.currentTimeMillis()
      (0 until countTxns) foreach { (x: Int) =>
        (0 until countElements) foreach { (y: Int) =>
          number += 1
          val record = new ProducerRecord[Array[Byte], Array[Byte]](
            kafkaInputNamePrefix + suffix, Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object])
          )
          producer.send(record)
        }
      }

      println(s"producer time: ${(System.currentTimeMillis() - s) / 1000}")
      producer.close()
    }

    _type match {
      case `tstreamMode` =>
        (1 to count).foreach(x => createTstreamData(countTxns, countElements, x.toString))
      case `kafkaMode` =>
        (1 to count).foreach(x => createKafkaData(countTxns, countElements, x.toString))
      case `commonMode` =>
        (1 to count).foreach(x => {
          createTstreamData(countTxns, countElements, x.toString)
          createKafkaData(countTxns, countElements, x.toString)
        })
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: $tstreamMode, $kafkaMode, $commonMode")
    }
  }

  def createInputTstreamConsumer(partitions: Int, suffix: String) = {
    createConsumer(tstreamInputNamePrefix + suffix, partitions)
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

  def createInputKafkaConsumer(inputCount: Int, partitionNumber: Int) = {

    val props = new Properties()
    props.put("bootstrap.servers", kafkaHosts)
    props.put("enable.auto.commit", "false")
    props.put("auto.offset.reset", "earliest")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)

    val topicPartitions = (0 until partitionNumber).flatMap(y => {
      (1 to inputCount).map(x => new TopicPartition(kafkaInputNamePrefix + x, y))
    }).asJava

    consumer.assign(topicPartitions)

    consumer
  }

  def createOutputConsumer(partitions: Int, suffix: String) = {
    createConsumer(tstreamOutputNamePrefix + suffix, partitions)
  }

  def createProducer(streamName: String, partitions: Int) = {
    val transactionGenerator = new LocalTransactionGenerator

    setProducerBindPort()
    setStreamOptions(streamName, partitions)

    tstreamFactory.getProducer[Array[Byte]](
      "producer for " + streamName,
      transactionGenerator,
      converter,
      (0 until partitions).toSet)
  }

  private def setProducerBindPort() = {
    tstreamFactory.setProperty(TSF_Dictionary.Producer.BIND_PORT, 8030)
  }

  private def createConsumer(streamName: String, partitions: Int): Consumer[Array[Byte]] = {
    val transactionGenerator = new LocalTransactionGenerator

    setStreamOptions(streamName, partitions)

    tstreamFactory.getConsumer[Array[Byte]](
      streamName,
      transactionGenerator,
      converter,
      (0 until partitions).toSet,
      Oldest)
  }

  protected def setStreamOptions(streamName: String, partitions: Int) = {
    tstreamFactory.setProperty(TSF_Dictionary.Stream.NAME, streamName)
    tstreamFactory.setProperty(TSF_Dictionary.Stream.PARTITIONS, partitions)
  }
}
