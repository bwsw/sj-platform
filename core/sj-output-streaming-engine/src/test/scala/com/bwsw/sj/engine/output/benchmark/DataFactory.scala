package com.bwsw.sj.engine.output.benchmark

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile

import com.bwsw.common._
import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model.module.{OutputInstance, Task}
import com.bwsw.sj.common.DAL.model.{ESService, _}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.module.ExecutionPlan
import com.bwsw.sj.common.utils.{ProviderLiterals, ServiceLiterals, _}
import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.tstreams.agents.consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer}
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder

import scala.collection.JavaConverters._

/**
  *
  *
  * @author Kseniya Tomskikh
  */
object DataFactory {
  private val agentsHost = "localhost"
  val zookeeperProviderName: String = "output-zookeeper-test-provider"
  val tstreamServiceName = "output-tstream-test-service"
  val tstreamInputName: String = "tstream-input"


  val esProviderName: String = "output-es-test-provider"
  val esServiceName: String = "output-es-test-service"
  val esStreamName: String = "es-output"

  val jdbcProviderName: String = "output-jdbc-test-provider"
  val jdbcServiceName: String = "output-jdbc-test-service"
  val jdbcStreamName: String = "jdbcoutput"
  val jdbcDriver: String = "mysql"

  val zookeeperServiceName: String = "output-zookeeper-test-service"
  val testNamespace = "test_namespace"

  val esIndex: String = "test_index_for_output_engine"
  val databaseName: String = "test_database_for_output_engine"

  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  val configService: GenericMongoService[ConfigurationSetting] = ConnectionRepository.getConfigService

  val esInstanceName: String = "test-es-instance-for-output-engine"
  val jdbcInstanceName: String = "test-jdbc-instance-for-output-engine"

  val objectSerializer = new ObjectSerializer()
  private val serializer: Serializer = new JsonSerializer

  private val esProviderHosts = System.getenv("ES_HOSTS").split(",").map(host => host.trim)
  private val jdbcHosts = System.getenv("JDBC_HOSTS").split(",").map(host => host.trim)
  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",").map(host => host.trim)
  private val zookeeperProvider = new Provider(zookeeperProviderName, zookeeperProviderName,
    zookeeperHosts, "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamService(tstreamServiceName, ServiceLiterals.tstreamsType,
    tstreamServiceName, zookeeperProvider, TestStorageServer.prefix, TestStorageServer.token)
  private val tstreamFactory = new TStreamsFactory()

  setTStreamFactoryProperties()

  val storageClient = tstreamFactory.getStorageClient()

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

  def createData(countTxns: Int, countElements: Int) = {
    val tStream: TStreamSjStream = new TStreamSjStream(
      tstreamInputName, "", 4, tstrqService,
      StreamLiterals.tstreamType, Array("tag"))

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
                        producer: Producer) = {
    var number = 0
    var string = "abc"
    (0 until countTxns) foreach { (x: Int) =>
      val transaction = producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        number % 8 match {
          case 0 => string = "abc"
          case 1 => string = "a'b"
          case 2 => string = "a\\"
          case 3 => string = "a\nc"
          case 4 => string = "a\""
          case 5 => string = """r"t"""
          case 6 => string = """r\t"""
          case 7 => string =
            """r
              |t
            """.stripMargin
        }
        println(s"write data $number, |$string|")
        val msg = objectSerializer.serialize((number, string).asInstanceOf[Object])
        transaction.send(msg)
      }
      println("checkpoint")
      transaction.checkpoint()
    }
  }

  def openEsConnection(outputStream: SjStream) = {
    val esService: ESService = outputStream.service.asInstanceOf[ESService]
    val hosts = esService.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)

    (client, esService)
  }

  def openJdbcConnection(outputStream: SjStream) = {
    val jdbcService: JDBCService = outputStream.service.asInstanceOf[JDBCService]
    val client = JdbcClientBuilder.
      setHosts(jdbcService.provider.hosts).
      setDriver(jdbcDriver).
      setUsername(jdbcService.provider.login).
      setPassword(jdbcService.provider.password).
      setDatabase(jdbcService.database).
      setTable(outputStream.name).
      build()

    (client, jdbcService)
  }

  def deleteIndex() = {
    if (streamService.get(esStreamName).isDefined) {
      val stream = streamService.get(esStreamName).get.asInstanceOf[ESSjStream]
      val (client, _) = openEsConnection(stream)
      client.deleteIndex(esIndex)

      client.close()
    }
  }

  def clearDatabase() = {
    if (streamService.get(jdbcStreamName).isDefined) {
      val stream = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCSjStream]
      val client = openJdbcConnection(stream)
      val sql = s"DROP TABLE `$jdbcStreamName`"
      client._1.execute(sql)
      client._1.close()
    }
  }

  def createProducer(stream: TStreamSjStream) = {
    setProducerBindPort()
    setStreamOptions(stream)

    tstreamFactory.getProducer(
      "producer for " + stream.name,
      (0 until stream.partitions).toSet)
  }

  private def setProducerBindPort() = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindPort, 8030)
  }

  def createConsumer(stream: TStreamSjStream): consumer.Consumer = {
    setStreamOptions(stream)

    tstreamFactory.getConsumer(
      stream.name,
      (0 until stream.partitions).toSet,
      Oldest)
  }

  protected def setStreamOptions(stream: TStreamSjStream) = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, stream.name)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, stream.partitions)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.description, stream.description)
  }

  def create_table: String = {
    s"CREATE TABLE $jdbcStreamName " +
    "(id VARCHAR(255) not NULL, " +
    " value INTEGER, " +
    " string_value VARCHAR(255), " +
    " txn BIGINT, " +
    " PRIMARY KEY ( id ))"
  }

  def close() = {
    tstreamFactory.close()
  }


  def createProviders() = {
    val esProvider = new Provider()
    esProvider.name = esProviderName
    esProvider.hosts = esProviderHosts
    esProvider.providerType = ProviderLiterals.elasticsearchType
    esProvider.login = ""
    esProvider.password = ""
    providerService.save(esProvider)

    providerService.save(zookeeperProvider)

    val jdbcProvider = new Provider()
    jdbcProvider.name = jdbcProviderName
    jdbcProvider.hosts = jdbcHosts
    jdbcProvider.providerType = ProviderLiterals.jdbcType
    jdbcProvider.login = "admin"
    jdbcProvider.password = "admin"
    providerService.save(jdbcProvider)
  }

  def createServices() = {
    val esProv: Provider = providerService.get(esProviderName).get
    val esService: ESService = new ESService()
    esService.name = esServiceName
    esService.serviceType = ServiceLiterals.elasticsearchType
    esService.description = "es service for benchmarks"
    esService.provider = esProv
    esService.index = esIndex
    esService.login = ""
    esService.password = ""
    serviceManager.save(esService)

    serviceManager.save(tstrqService)

    val zkService = new ZKService()
    zkService.name = zookeeperServiceName
    zkService.serviceType = ServiceLiterals.zookeeperType
    zkService.description = "zk service for benchmarks"
    zkService.provider = zookeeperProvider
    zkService.namespace = testNamespace
    serviceManager.save(zkService)

    val jdbcProvider: Provider = providerService.get(jdbcProviderName).get
    val jdbcService = new JDBCService()
    jdbcService.name = jdbcServiceName
    jdbcService.driver = jdbcDriver
    jdbcService.description = "jdbc service for benchmark"
    jdbcService.provider = jdbcProvider
    jdbcService.database = databaseName
    serviceManager.save(jdbcService)
  }

  def mapping: XContentBuilder = jsonBuilder()
    .startObject()
      .startObject("properties")
        .startObject("txn").field("type", "long").endObject()
        .startObject("test-date").field("type", "date").endObject()
        .startObject("value").field("type", "integer").endObject()
        .startObject("string-value").field("type", "string").endObject()
      .endObject()
    .endObject()

  def createIndex() = {
    val stream = streamService.get(esStreamName).get.asInstanceOf[ESSjStream]
    val esClient = openEsConnection(stream)
    esClient._1.createIndex(esIndex)
    createIndexMapping(mapping)
  }

  def createIndexMapping(mapping: XContentBuilder) = {
    val stream = streamService.get(esStreamName).get.asInstanceOf[ESSjStream]
    val esClient = openEsConnection(stream)
    esClient._1.createMapping(esIndex, esStreamName, mapping)
  }

  def createTable() = {
    val stream = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCSjStream]
    val client = openJdbcConnection(stream)
    client._1.execute(create_table)
  }

  def createStreams(partitions: Int) = {
    val esService = serviceManager.get(esServiceName).get.asInstanceOf[ESService]
    val esStream: ESSjStream = new ESSjStream()
    esStream.name = esStreamName
    esStream.description = "es stream for benchmarks"
    esStream.streamType = StreamLiterals.esOutputType
    esStream.service = esService
    esStream.tags = Array("tag1")
    streamService.save(esStream)

    val tService: TStreamService = serviceManager.get(tstreamServiceName).get.asInstanceOf[TStreamService]
    val tStream: TStreamSjStream = new TStreamSjStream()
    tStream.name = tstreamInputName
    tStream.description = "t-stream for benchmarks"
    tStream.streamType = StreamLiterals.tstreamType
    tStream.service = tService
    tStream.tags = Array("tag1")
    tStream.partitions = partitions
    streamService.save(tStream)

    val jdbcService: JDBCService = serviceManager.get(jdbcServiceName).get.asInstanceOf[JDBCService]
    val jdbcStream: JDBCSjStream = new JDBCSjStream()
    jdbcStream.name = jdbcStreamName
    jdbcStream.primary = "test"
    jdbcStream.description = "jdbc stream for benchmarks"
    jdbcStream.streamType = StreamLiterals.jdbcOutputType
    jdbcStream.service = jdbcService
    jdbcStream.tags = Array("tag1")
    streamService.save(jdbcStream)

    storageClient.createStream(
      tstreamInputName,
      partitions,
      60000,
      "")
  }

  def createInstance(instanceName: String, checkpointMode: String, checkpointInterval: Long,
                     streamName: String, moduleName: String) = {

    val task1 = new Task()
    task1.inputs = Map(tstreamInputName -> Array(0, 3)).asJava
    val executionPlan = new ExecutionPlan(Map(instanceName + "-task0" -> task1).asJava)

    val instance = new OutputInstance()
    instance.name = instanceName
    instance.moduleType = EngineLiterals.outputStreamingType
    instance.moduleName = moduleName
    instance.moduleVersion = "1.0"
    instance.status = EngineLiterals.started
    instance.description = "some description of test instance"
    instance.inputs = Array(tstreamInputName)
    instance.outputs = Array(streamName)
    instance.checkpointMode = checkpointMode
    instance.checkpointInterval = checkpointInterval
    instance.options = """{"hey": "hey"}"""
    instance.startFrom = EngineLiterals.oldestStartMode
    instance.executionPlan = executionPlan
    instance.engine = "com.bwsw.output.streaming.engine-1.0"
    instance.coordinationService = serviceManager.get(zookeeperServiceName).get.asInstanceOf[ZKService]

    instanceService.save(instance)
  }

  def deleteInstance(instanceName: String) = {
    instanceService.delete(instanceName)
  }

  def deleteStreams() = {
    streamService.delete(esStreamName)
    streamService.delete(tstreamInputName)
    streamService.delete(jdbcStreamName)

    storageClient.deleteStream(esStreamName)
    storageClient.deleteStream(tstreamInputName)
    storageClient.deleteStream(jdbcStreamName)
  }

  def deleteServices() = {
    serviceManager.delete(esServiceName)
    serviceManager.delete(tstreamServiceName)
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(jdbcServiceName)
  }

  def deleteProviders() = {
    providerService.delete(zookeeperProviderName)
    providerService.delete(esProviderName)
    providerService.delete(jdbcProviderName)
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