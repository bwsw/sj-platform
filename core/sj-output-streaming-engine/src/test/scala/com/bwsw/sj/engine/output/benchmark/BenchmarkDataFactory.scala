package com.bwsw.sj.engine.output.benchmark

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.common.traits.Serializer
import com.bwsw.common.{ElasticsearchClient, JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{OutputInstance, Task}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.module.ExecutionPlan
import com.bwsw.sj.common.utils.{GeneratorLiterals, ProviderLiterals, ServiceLiterals, _}
import com.bwsw.tstreams.agents.consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}
import com.bwsw.tstreams.generator.LocalTransactionGenerator
import com.bwsw.tstreams.services.BasicStreamService

import scala.collection.JavaConverters._

/**
 *
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
  val configService: GenericMongoService[ConfigurationSetting] = ConnectionRepository.getConfigService

  val objectSerializer = new ObjectSerializer()
  private val serializer: Serializer = new JsonSerializer
  private val cassandraHost = System.getenv("CASSANDRA_HOST")
  private val cassandraPort = System.getenv("CASSANDRA_PORT").toInt
  private val cassandraFactory = new CassandraFactory()
  private val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()

  private def setTStreamFactoryProperties() = {
    val inputStreamService = serviceManager.get(tServiceName).get.asInstanceOf[TStreamService]
    setMetadataClusterProperties(inputStreamService)
    setDataClusterProperties(inputStreamService)
    setCoordinationOptions(inputStreamService)
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
    val agentsHost = "localhost"
    tstreamFactory.setProperty(TSF_Dictionary.Producer.BIND_HOST, agentsHost)
    tstreamFactory.setProperty(TSF_Dictionary.Consumer.Subscriber.BIND_HOST, agentsHost)
  }

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  def createData(countTxns: Int, countElements: Int) = {
    val tStream: TStreamSjStream = streamService.get(tStreamName).asInstanceOf[TStreamSjStream]
    val producer = createProducer(tStream)
    val s = System.currentTimeMillis()
    writeData(countTxns,
      countElements,
      producer)
    println(s"producer time: ${(System.currentTimeMillis - s) / 1000}")
    producer.stop()
  }

  private def writeData(countTxns: Int,
                        countElements: Int,
                        producer: Producer[Array[Byte]]) = {
    var number = 0
    (0 until countTxns) foreach { (x: Int) =>
      val transaction = producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        println("write data " + number)
        val msg = objectSerializer.serialize(number.asInstanceOf[Object])
        transaction.send(msg)
      }
      println("checkpoint")
      transaction.checkpoint()
    }
  }

  def openDbConnection(outputStream: SjStream) = {
    val esService: ESService = outputStream.service.asInstanceOf[ESService]
    val hosts = esService.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)

    (client, esService)
  }

  def clearEsStream() = {
    val stream = streamService.get(esStreamName).asInstanceOf[ESSjStream]
    val (client, service) = openDbConnection(stream)
    val outputData = client.search(service.index, stream.name)

    outputData.getHits.foreach { hit =>
      val id = hit.getId
      client.deleteDocumentByTypeAndId(service.index, stream.name, id)
    }

    client.close()
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

  def createConsumer(stream: TStreamSjStream): consumer.Consumer[Array[Byte]] = {
    val transactionGenerator = new LocalTransactionGenerator

    setStreamOptions(stream)

    tstreamFactory.getConsumer[Array[Byte]](
      stream.name,
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

  def open() = cassandraFactory.open(Set((cassandraHost, cassandraPort)))

  def prepareCassandra(keyspace: String) = {
    cassandraFactory.createKeyspace(keyspace)
    cassandraFactory.createMetadataTables(keyspace)
    cassandraFactory.createDataTable(keyspace)
  }

  def close() = {
    cassandraFactory.close()
    tstreamFactory.close()
  }

  def createProviders() = {
    val esProvider = new Provider()
    esProvider.name = esProviderName
    esProvider.hosts = Array("127.0.0.1:9300")
    esProvider.providerType = ProviderLiterals.elasticsearchType
    esProvider.login = ""
    esProvider.password = ""
    providerService.save(esProvider)

    val metadataProvider = new Provider()
    metadataProvider.name = metadataProviderName
    metadataProvider.hosts = Array("127.0.0.1:9042")
    metadataProvider.providerType = ProviderLiterals.cassandraType
    metadataProvider.login = ""
    metadataProvider.password = ""
    providerService.save(metadataProvider)

    val dataProvider = new Provider()
    dataProvider.name = dataProviderName
    dataProvider.hosts = Array("127.0.0.1:3000", "127.0.0.1:3001")
    dataProvider.providerType = ProviderLiterals.aerospikeType
    dataProvider.login = ""
    dataProvider.password = ""
    providerService.save(dataProvider)

    val lockProvider = new Provider()
    lockProvider.name = lockProviderName
    lockProvider.hosts = Array("127.0.0.1:2181")
    lockProvider.providerType = ProviderLiterals.zookeeperType
    lockProvider.login = ""
    lockProvider.password = ""
    providerService.save(lockProvider)
  }

  def createServices() = {
    val esProv: Provider = providerService.get(esProviderName).get
    val esService: ESService = new ESService()
    esService.name = esServiceName
    esService.serviceType = ServiceLiterals.elasticsearchType
    esService.description = "es service for benchmarks"
    esService.provider = esProv
    esService.index = "bench"
    esService.login = ""
    esService.password = ""
    serviceManager.save(esService)

    val metadataProvider: Provider = providerService.get(metadataProviderName).get
    val dataProvider: Provider = providerService.get(dataProviderName).get
    val lockProvider: Provider = providerService.get(lockProviderName).get
    val tStreamService: TStreamService = new TStreamService()
    tStreamService.name = tServiceName
    tStreamService.serviceType = ServiceLiterals.tstreamsType
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
    zkService.serviceType = ServiceLiterals.zookeeperType
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
    tStream.generator = new Generator(GeneratorLiterals.localType)
    streamService.save(tStream)

    val metadataHosts = splitHosts(tService.metadataProvider.hosts)
    val cassandraFactory = new CassandraFactory()
    cassandraFactory.open(metadataHosts)
    val metadataStorage = cassandraFactory.getMetadataStorage(tService.metadataNamespace)

    val aerospikeFactory = new AerospikeFactory
    val namespace = tService.dataNamespace
    val dataHosts = splitHosts(tService.dataProvider.hosts)
    val dataStorage = aerospikeFactory.getDataStorage(namespace, dataHosts)

    BasicStreamService.createStream(tStreamName,
      partitions,
      60000,
      "", metadataStorage,
      dataStorage)

    cassandraFactory.close()
  }

  def createInstance(instanceName: String, checkpointMode: String, checkpointInterval: Long) = {

    val task1 = new Task()
    task1.inputs = Map(tStreamName -> Array(0, 1)).asJava
    val task2 = new Task()
    task2.inputs = Map(tStreamName -> Array(2, 3)).asJava
    val executionPlan = new ExecutionPlan(Map((instanceName + "-task0", task1), (instanceName + "-task1", task2)).asJava)

    val instance = new OutputInstance()
    instance.name = instanceName
    instance.moduleType = EngineLiterals.outputStreamingType
    instance.moduleName = "com.bwsw.stub.output-bench-test"
    instance.moduleVersion = "1.0"
    instance.status = EngineLiterals.started
    instance.description = "some description of test instance"
    instance.inputs = Array(tStreamName)
    instance.outputs = Array(esStreamName)
    instance.checkpointMode = checkpointMode
    instance.checkpointInterval = checkpointInterval
    instance.parallelism = 2
    instance.options = """{"hey": "hey"}"""
    instance.startFrom = EngineLiterals.oldestStartMode
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

  def cassandraDestroy(keyspace: String) = {
    cassandraFactory.dropKeyspace(keyspace)
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

  private def splitHosts(hosts: Array[String]) = {
    hosts.map(s => {
      val address = s.split(":")
      (address(0), address(1).toInt)
    }).toSet
  }
}
