package com.bwsw.sj.engine.output.benchmark

import java.io.{BufferedReader, File, InputStreamReader}
import java.net.{InetAddress, InetSocketAddress}
import java.util.jar.JarFile

import com.aerospike.client.Host
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.common.traits.Serializer
import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{ExecutionPlan, OutputInstance, Task}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.engine.core.utils.CassandraHelper._
import com.bwsw.tstreams.agents.consumer.Offsets.Oldest
import com.bwsw.tstreams.agents.consumer.{Consumer, ConsumerOptions}
import com.bwsw.tstreams.agents.producer.DataInsertType.BatchInsert
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer, CoordinationOptions, Options}
import com.bwsw.tstreams.common.CassandraConnectorConf
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.producer.transport.impl.TcpTransport

import com.bwsw.tstreams.data.aerospike
import com.bwsw.tstreams.env.TSF_Dictionary
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.datastax.driver.core.Cluster
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

import scala.collection.JavaConverters._

/**
 * Created: 20/06/2016
 *
 * @author Kseniya Tomskikh
 */
object BenchmarkDataFactory {
  val metadataProviderName: String = "test-metprov-1"
  val metadataNamespace: String = "bench"
  val dataProviderName: String = "test-dataprov-1"
  val dataNamespace: String = "test"
  val lockProviderName: String = "test-lockprov-1"
  val lockNamespace: String = "bench"
  val tServiceName: String = "test-tserv-1"
  val tStreamName: String = "test-tstr-1"

  val esProviderName: String = "test-esprov-1"
  val esServiceName: String = "test-esserv-1"
  val esStreamName: String = "test-es-1"

  val zkServiceName: String = "test-zkserv-1"

  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  val configService: GenericMongoService[ConfigSetting] = ConnectionRepository.getConfigService

  val objectSerializer = new ObjectSerializer()
  private val serializer: Serializer = new JsonSerializer

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  private val cluster = Cluster.builder().addContactPoint(cassandraHost).build()
  private val session = cluster.connect()

  def createData(countTxns: Int, countElements: Int) = {
    val tStream: TStreamSjStream = streamService.get(tStreamName).asInstanceOf[TStreamSjStream]
    val tStreamService = tStream.service.asInstanceOf[TStreamService]
    val metadataStorageFactory = new MetadataStorageFactory
    val cassandraConnectorConf = CassandraConnectorConf.apply(tStreamService.metadataProvider.hosts.map { addr =>
      val parts = addr.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toSet)
    val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(cassandraConnectorConf, tStreamService.metadataNamespace)

    val dataStorageFactory = new aerospike.Factory
    val dataStorageHosts = tStreamService.dataProvider.hosts.map { addr =>
      val parts = addr.split(":")
      new Host(parts(0), parts(1).toInt)
    }.toList
    val options = new aerospike.Options(tStreamService.dataNamespace, dataStorageHosts)
    val dataStorage: aerospike.Storage = dataStorageFactory.getInstance(options)

    val producer = createProducer(metadataStorage, dataStorage, tStream.partitions)

    val s = System.nanoTime

    writeData(countTxns,
      countElements,
      producer)

    println(s"producer time: ${(System.nanoTime - s) / 1000000}")

    producer.stop()

    dataStorageFactory.closeFactory()
    metadataStorageFactory.closeFactory()
  }

  private def writeData(countTxns: Int,
                        countElements: Int,
                        producer: Producer[Array[Byte]]) = {
    var number = 0
    (0 until countTxns) foreach { (x: Int) =>
      val txn = producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        println("write data " + number)
        val msg = objectSerializer.serialize(number.asInstanceOf[Object])
        txn.send(msg)
      }
      println("checkpoint")
      txn.checkpoint()
    }
  }

  def openDbConnection(outputStream: SjStream): (TransportClient, ESService) = {
    val esService: ESService = outputStream.service.asInstanceOf[ESService]
    val client: TransportClient = TransportClient.builder().build()
    esService.provider.hosts.foreach { host =>
      val parts = host.split(":")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(parts(0)), parts(1).toInt))
    }
    (client, esService)
  }

  def clearEsStream() = {
    val stream = streamService.get(esStreamName).asInstanceOf[ESSjStream]
    val (client, service) = openDbConnection(stream)
    val esRequest: SearchResponse = client
      .prepareSearch(service.index)
      .setTypes(stream.name)
      .setSize(2000)
      .execute()
      .get()
    val outputData = esRequest.getHits

    outputData.getHits.foreach { hit =>
      val id = hit.getId
      client.prepareDelete(service.index, stream.name, id).execute().actionGet()
    }
  }

  private def createProducer(metadataStorage: MetadataStorage, dataStorage: aerospike.Storage, partitions: Int) = {
    val tStream =
      BasicStreamService.loadStream(tStreamName, metadataStorage, dataStorage)

    val coordinationSettings = new CoordinationOptions(
      agentAddress = s"localhost:8030",
      zkHosts = List(new InetSocketAddress("localhost", 2181)),
      zkRootPath = "/unit",
      zkSessionTimeout = 7000,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport("localhost:8030",
        TSF_Dictionary.Producer.TRANSPORT_TIMEOUT.toInt * 1000),
      zkConnectionTimeout = 7000)

    val roundRobinPolicy = new RoundRobinPolicy(tStream, (0 until partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val producerOptions = new Options[Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      roundRobinPolicy,
      BatchInsert(5),
      timeUuidGenerator,
      coordinationSettings,
      converter)

    new Producer[Array[Byte]]("producer for " + tStream.name, tStream, producerOptions)
  }

  def createConsumer(stream: TStreamSjStream,
                     address: String,
                     metadataStorage: MetadataStorage,
                     dataStorage: aerospike.Storage): Consumer[Array[Byte]] = {

    val tStream =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

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

  def prepareCassandra(keyspace: String) = {
    createKeyspace(session, keyspace)
    createMetadataTables(session, keyspace)
  }

  def createProviders() = {
    val esProvider = new Provider()
    esProvider.name = esProviderName
    esProvider.hosts = Array("127.0.0.1:9300")
    esProvider.providerType = "ES"
    esProvider.login = ""
    esProvider.password = ""
    providerService.save(esProvider)

    val metadataProvider = new Provider()
    metadataProvider.name = metadataProviderName
    metadataProvider.hosts = Array("127.0.0.1:9042")
    metadataProvider.providerType = "cassandra"
    metadataProvider.login = ""
    metadataProvider.password = ""
    providerService.save(metadataProvider)

    val dataProvider = new Provider()
    dataProvider.name = dataProviderName
    dataProvider.hosts = Array("127.0.0.1:3000", "127.0.0.1:3001")
    dataProvider.providerType = "aerospike"
    dataProvider.login = ""
    dataProvider.password = ""
    providerService.save(dataProvider)

    val lockProvider = new Provider()
    lockProvider.name = lockProviderName
    lockProvider.hosts = Array("127.0.0.1:2181")
    lockProvider.providerType = "zookeeper"
    lockProvider.login = ""
    lockProvider.password = ""
    providerService.save(lockProvider)
  }

  def createServices() = {
    val esProv: Provider = providerService.get(esProviderName)
    val esService: ESService = new ESService()
    esService.name = esServiceName
    esService.serviceType = "ESInd"
    esService.description = "es service for benchmarks"
    esService.provider = esProv
    esService.index = "bench"
    esService.login = ""
    esService.password = ""
    serviceManager.save(esService)

    val metadataProvider: Provider = providerService.get(metadataProviderName)
    val dataProvider: Provider = providerService.get(dataProviderName)
    val lockProvider: Provider = providerService.get(lockProviderName)
    val tStreamService: TStreamService = new TStreamService()
    tStreamService.name = tServiceName
    tStreamService.serviceType = "TstrQ"
    tStreamService.description = "t-streams service for benchmarks"
    tStreamService.metadataProvider = metadataProvider
    tStreamService.metadataNamespace = metadataNamespace
    tStreamService.dataProvider = dataProvider
    tStreamService.dataNamespace = dataNamespace
    tStreamService.lockProvider = lockProvider
    tStreamService.lockNamespace = lockNamespace
    serviceManager.save(tStreamService)

    val zkService = new ZKService()
    zkService.name = zkServiceName
    zkService.serviceType = "ZKCoord"
    zkService.description = "zk service for benchmarks"
    zkService.provider = lockProvider
    zkService.namespace = "bench"
    serviceManager.save(zkService)
  }

  def createStreams(partitions: Int) = {
    val esService: ESService = serviceManager.get(esServiceName).asInstanceOf[ESService]
    val esStream: ESSjStream = new ESSjStream()
    esStream.name = esStreamName
    esStream.description = "es stream for benchmarks"
    esStream.streamType = "elasticsearch-output"
    esStream.service = esService
    esStream.tags = Array("tag1")
    streamService.save(esStream)

    val tService: TStreamService = serviceManager.get(tServiceName).asInstanceOf[TStreamService]
    val tStream: TStreamSjStream = new TStreamSjStream()
    tStream.name = tStreamName
    tStream.description = "t-stream for benchmarks"
    tStream.streamType = "stream.t-stream"
    tStream.service = tService
    tStream.tags = Array("tag1")
    tStream.partitions = partitions
    tStream.generator = new Generator("local")
    streamService.save(tStream)

    val metadataStorageFactory = new MetadataStorageFactory
    val cassandraConnectorConf = CassandraConnectorConf.apply(tService.metadataProvider.hosts.map { addr =>
      val parts = addr.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toSet)
    val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(cassandraConnectorConf, tService.metadataNamespace)

    val dataStorageFactory = new aerospike.Factory
    val dataStorageHosts = tService.dataProvider.hosts.map { addr =>
      val parts = addr.split(":")
      new Host(parts(0), parts(1).toInt)
    }.toList
    val options = new aerospike.Options(tService.dataNamespace, dataStorageHosts)
    val dataStorage: aerospike.Storage = dataStorageFactory.getInstance(options)

    BasicStreamService.createStream(tStreamName,
      partitions,
      configService.get(ConfigConstants.streamTTLTag).value.toInt,
      "", metadataStorage,
      dataStorage)

    metadataStorageFactory.closeFactory()
    dataStorageFactory.closeFactory()
  }

  def createInstance(instanceName: String, checkpointMode: String, checkpointInterval: Long) = {

    val task1 = new Task()
    task1.inputs = Map(tStreamName -> Array(0, 1)).asJava
    val task2 = new Task()
    task2.inputs = Map(tStreamName -> Array(2, 3)).asJava
    val executionPlan = new ExecutionPlan(Map((instanceName + "-task0", task1), (instanceName + "-task1", task2)).asJava)

    val instance = new OutputInstance()
    instance.name = instanceName
    instance.moduleType = "output-streaming"
    instance.moduleName = "com.bwsw.stub.output-bench-test"
    instance.moduleVersion = "1.0"
    instance.status = "started"
    instance.description = "some description of test instance"
    instance.inputs = Array(tStreamName)
    instance.outputs = Array(esStreamName)
    instance.checkpointMode = checkpointMode
    instance.checkpointInterval = checkpointInterval
    instance.parallelism = 2
    instance.options = """{"hey": "hey"}"""
    instance.startFrom = "oldest"
    instance.perTaskCores = 0.1
    instance.perTaskRam = 64
    instance.performanceReportingInterval = 10000
    instance.executionPlan = executionPlan
    instance.engine = "com.bwsw.output.streaming.engine-1.0"
    instance.coordinationService = serviceManager.get(zkServiceName).asInstanceOf[ZKService]

    instanceService.save(instance)
  }

  def deleteInstance(instanceName: String) = {
    instanceService.delete(instanceName)
  }

  def deleteStreams() = {
    streamService.delete(esStreamName)
    streamService.delete(tStreamName)
  }

  def deleteServices() = {
    serviceManager.delete(esServiceName)
    serviceManager.delete(tServiceName)
    serviceManager.delete(zkServiceName)
  }

  def deleteProviders() = {
    providerService.delete(dataProviderName)
    providerService.delete(metadataProviderName)
    providerService.delete(lockProviderName)
    providerService.delete(esProviderName)
  }

  def close() = {
    session.close()
    cluster.close()
  }

  def cassandraDestroy(keyspace: String) = {
    session.execute(s"DROP KEYSPACE $keyspace")
  }

  def uploadModule(moduleJar: File) = {
    val builder = new StringBuilder
    val jar = new JarFile(moduleJar)
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

    fileStorage.put(moduleJar, moduleJar.getName, specification, "module")
  }

  def deleteModule(filename: String) = {
    fileStorage.delete(filename)
  }

}
