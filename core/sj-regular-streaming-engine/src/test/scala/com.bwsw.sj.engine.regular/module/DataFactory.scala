package com.bwsw.sj.engine.regular.module

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.Properties
import java.util.jar.JarFile

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.dal.model.module.{Instance, RegularInstance, Task}
import com.bwsw.sj.common.dal.model.provider.Provider
import com.bwsw.sj.common.dal.model.service.{KafkaService, Service, TStreamService, ZKService}
import com.bwsw.sj.common.dal.model.stream.{KafkaSjStream, SjStream, TStreamSjStream}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.dal.service.GenericMongoRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.rest.model.module.ExecutionPlan
import com.bwsw.sj.common.utils._
import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object DataFactory {
  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",")
  private val kafkaHosts = System.getenv("KAFKA_HOSTS")
  val kafkaMode = "kafka"
  val tstreamMode = "tstream"
  val commonMode = "both"
  private val agentsHost = "localhost"
  private val testNamespace = "test_namespace_for_regular_engine"
  private val instanceName = "test-instance-for-regular-engine"
  private val tstreamInputNamePrefix = "regular-tstream-input"
  private val tstreamOutputNamePrefix = "regular-tstream-output"
  private val kafkaInputNamePrefix = "regular-kafka-input"
  private val kafkaProviderName = "regular-kafka-test-provider"
  private val zookeeperProviderName = "regular-zookeeper-test-provider"
  private val tstreamServiceName = "regular-tstream-test-service"
  private val kafkaServiceName = "regular-kafka-test-service"
  private val zookeeperServiceName = "regular-zookeeper-test-service"
  private val replicationFactor = 1
  private var instanceInputs: Array[String] = Array()
  private var instanceOutputs: Array[String] = Array()
  private val task: Task = new Task()
  private val serializer = new JsonSerializer()
  private val objectSerializer = new ObjectSerializer()
  private val zookeeperProvider = new Provider(zookeeperProviderName, zookeeperProviderName, zookeeperHosts, "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamService(tstreamServiceName, tstreamServiceName, zookeeperProvider,
    TestStorageServer.prefix, TestStorageServer.token)
  private val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()
  private val storageClient = tstreamFactory.getStorageClient()

  val inputCount = 2
  val outputCount = 2
  val partitions = 4

  private def setTStreamFactoryProperties() = {
    setAuthOptions(tstrqService)
    setStorageOptions(tstrqService)
    setCoordinationOptions(tstrqService)
    setBindHostForAgents()
  }

  private def setAuthOptions(tStreamService: TStreamService) = {
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Auth.key, tStreamService.token)
  }

  private def setStorageOptions(tStreamService: TStreamService) = {
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, tStreamService.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.prefix, tStreamService.prefix)
  }

  private def setCoordinationOptions(tStreamService: TStreamService) = {
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.endpoints, tStreamService.provider.hosts.mkString(","))
  }

  private def setBindHostForAgents() = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindHost, agentsHost)
    tstreamFactory.setProperty(ConfigurationOptions.Consumer.Subscriber.bindHost, agentsHost)
  }

  def createProviders(providerService: GenericMongoRepository[Provider]) = {
    val kafkaProvider = new Provider(kafkaProviderName, kafkaProviderName, kafkaHosts.split(","), "", "", ProviderLiterals.kafkaType)
    providerService.save(kafkaProvider)

    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoRepository[Provider]) = {
    providerService.delete(kafkaProviderName)
    providerService.delete(zookeeperProviderName)
  }

  def createServices(serviceManager: GenericMongoRepository[Service], providerService: GenericMongoRepository[Provider]) = {
    val zkService = new ZKService(zookeeperServiceName, zookeeperServiceName, zookeeperProvider, testNamespace)
    serviceManager.save(zkService)

    val kafkaProv = providerService.get(kafkaProviderName).get
    val kafkaService = new KafkaService(kafkaServiceName, kafkaServiceName, kafkaProv, zookeeperProvider, testNamespace)
    serviceManager.save(kafkaService)

    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoRepository[Service]) = {
    serviceManager.delete(kafkaServiceName)
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(tstreamServiceName)
  }

  def createStreams(sjStreamService: GenericMongoRepository[SjStream], serviceManager: GenericMongoRepository[Service],
                    partitions: Int, _type: String, inputCount: Int, outputCount: Int) = {
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

  def deleteStreams(streamService: GenericMongoRepository[SjStream],
                    _type: String,
                    serviceManager: GenericMongoRepository[Service],
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

  private def createInputTStream(sjStreamService: GenericMongoRepository[SjStream], serviceManager: GenericMongoRepository[Service], partitions: Int, suffix: String) = {
    val s1 = new TStreamSjStream(tstreamInputNamePrefix + suffix,
      tstrqService,
      partitions,
      tstreamInputNamePrefix + suffix,
      false,
      Array("input")
    )

    sjStreamService.save(s1)

    storageClient.createStream(
      tstreamInputNamePrefix + suffix,
      partitions,
      1000 * 60,
      "description of test input tstream"
    )
  }

  private def createOutputTStream(sjStreamService: GenericMongoRepository[SjStream], serviceManager: GenericMongoRepository[Service], partitions: Int, suffix: String) = {
    val s2 = new TStreamSjStream(tstreamOutputNamePrefix + suffix,
      tstrqService,
      partitions,
      tstreamOutputNamePrefix + suffix,
      false,
      Array("output", "some tags")
    )

    sjStreamService.save(s2)

    storageClient.createStream(
      tstreamOutputNamePrefix + suffix,
      partitions,
      10 * 60,
      tstreamOutputNamePrefix + suffix
    )
  }

  private def deleteInputTStream(streamService: GenericMongoRepository[SjStream], suffix: String) = {
    streamService.delete(tstreamInputNamePrefix + suffix)

    storageClient.deleteStream(tstreamInputNamePrefix + suffix)
  }

  private def deleteOutputTStream(streamService: GenericMongoRepository[SjStream], suffix: String) = {
    streamService.delete(tstreamOutputNamePrefix + suffix)

    storageClient.deleteStream(tstreamOutputNamePrefix + suffix)

  }

  private def createKafkaStream(sjStreamService: GenericMongoRepository[SjStream], serviceManager: GenericMongoRepository[Service], partitions: Int, suffix: String) = {
    val kService = serviceManager.get(kafkaServiceName).get.asInstanceOf[KafkaService]

    val kafkaSjStream = new KafkaSjStream(kafkaInputNamePrefix + suffix,
      kService,
      partitions,
      replicationFactor,
      kafkaInputNamePrefix + suffix,
      false,
      Array(kafkaInputNamePrefix)
    )

    sjStreamService.save(kafkaSjStream)

    val zkHost = kService.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConnectionRepository.getConfigService.get(ConfigLiterals.zkSessionTimeoutTag).get.value.toInt
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    AdminUtils.createTopic(zkUtils, kafkaSjStream.name, partitions, replicationFactor)
  }

  private def deleteKafkaStream(streamService: GenericMongoRepository[SjStream], serviceManager: GenericMongoRepository[Service], suffix: String) = {
    val kService = serviceManager.get(kafkaServiceName).get.asInstanceOf[KafkaService]
    val zkHost = kService.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConnectionRepository.getConfigService.get(ConfigLiterals.zkSessionTimeoutTag).get.value.toInt
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    AdminUtils.deleteTopic(zkUtils, kafkaInputNamePrefix + suffix)
    streamService.delete(kafkaInputNamePrefix + suffix)
  }


  def createInstance(serviceManager: GenericMongoRepository[Service],
                     instanceService: GenericMongoRepository[Instance],
                     checkpointInterval: Int,
                     stateManagement: String = EngineLiterals.noneStateMode,
                     stateFullCheckpoint: Int = 0) = {
    import scala.collection.JavaConverters._

    val instance = new RegularInstance(instanceName, EngineLiterals.regularStreamingType,
      "regular-streaming-stub", "1.0", "com.bwsw.regular.streaming.engine-1.0",
      serviceManager.get(zookeeperServiceName).get.asInstanceOf[ZKService], EngineLiterals.everyNthMode)
    instance.status = EngineLiterals.started
    instance.inputs = instanceInputs
    instance.outputs = instanceOutputs
    instance.checkpointInterval = checkpointInterval
    instance.stateManagement = stateManagement
    instance.stateFullCheckpoint = stateFullCheckpoint
    instance.startFrom = EngineLiterals.oldestStartMode
    instance.executionPlan = new ExecutionPlan(Map(instanceName + "-task0" -> task, instanceName + "-task1" -> task).asJava)

    instanceService.save(instance)
  }

  def deleteInstance(instanceService: GenericMongoRepository[Instance]) = {
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
        val result = Try {
          var line = reader.readLine
          while (line != null) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        }
        reader.close()
        result match {
          case Success(_) =>
          case Failure(e) => throw e
        }
      }
    }

    val specification = serializer.deserialize[Map[String, Any]](builder.toString())

    storage.put(file, file.getName, specification, "module")
  }

  def deleteModule(storage: FileStorage, filename: String) = {
    storage.delete(filename)
  }

  def createData(countTxns: Int, countElements: Int, partitions: Int, _type: String, count: Int) = {
    val policy = producer.NewTransactionProducerPolicy.ErrorIfOpened
    val props = new Properties()
    props.put("bootstrap.servers", kafkaHosts)
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")

    def createTstreamData(countTxns: Int, countElements: Int, suffix: String) = {
      val producer = createProducer(tstreamInputNamePrefix + suffix, partitions)
      var number = 0

      (0 until countTxns) foreach { (x: Int) =>
        val transaction = producer.newTransaction(policy)
        (0 until countElements) foreach { (y: Int) =>
          number += 1
          transaction.send(objectSerializer.serialize(number.asInstanceOf[Object]))
        }
        transaction.checkpoint()
      }

      producer.stop()
    }

    def createKafkaData(countTxns: Int, countElements: Int, suffix: String) = {
      val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
      var number = 0

      (0 until countTxns) foreach { (x: Int) =>
        (0 until countElements) foreach { (y: Int) =>
          number += 1
          val record = new ProducerRecord[Array[Byte], Array[Byte]](
            kafkaInputNamePrefix + suffix, Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object])
          )
          producer.send(record)
        }
      }

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

  def createStateConsumer(streamService: GenericMongoRepository[SjStream]) = {
    val name = instanceName + "-task0" + "_state"
    val partitions = 1

    setStreamOptions(name, partitions)

    tstreamFactory.getConsumer(
      name,
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
    setProducerBindPort()
    setStreamOptions(streamName, partitions)

    tstreamFactory.getProducer(
      "producer for " + streamName,
      (0 until partitions).toSet)
  }

  private def setProducerBindPort() = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindPort, 8030)
  }

  private def createConsumer(streamName: String, partitions: Int): Consumer = {
    setStreamOptions(streamName, partitions)

    tstreamFactory.getConsumer(
      streamName,
      (0 until partitions).toSet,
      Oldest)
  }

  protected def setStreamOptions(streamName: String, partitions: Int) = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, streamName)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, partitions)
  }
}