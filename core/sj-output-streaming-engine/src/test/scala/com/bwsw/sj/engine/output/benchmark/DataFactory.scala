package com.bwsw.sj.engine.output.benchmark

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile

import com.bwsw.common._
import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.config.BenchmarkConfigNames
import com.bwsw.sj.common.dal.model._
import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, Task}
import com.bwsw.sj.common.dal.model.provider.{JDBCProviderDomain, ProviderDomain}
import com.bwsw.sj.common.dal.model.service._
import com.bwsw.sj.common.dal.model.stream._
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance.OutputInstance
import com.bwsw.sj.common.utils.{ProviderLiterals, _}
import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.tstreams.agents.consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer.{NewProducerTransactionPolicy, Producer}
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.http.HttpVersion
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  *
  *
  * @author Kseniya Tomskikh
  */
object DataFactory {
  private val config = ConfigFactory.load()
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

  val pathToRestModule = "./contrib/stubs/sj-stub-rest-output-streaming/target/scala-2.12/sj-stub-rest-output-streaming-1.0-SNAPSHOT.jar"
  val restProviderName = "output-rest-test-provider"
  val restServiceName = "output-rest-test-service"
  val restStreamName = "rest-output"
  val restHeaders = Map(
    "header1" -> "value1",
    "header2" -> "value2"
  ).asJava
  val restHttpVersion = HttpVersion.HTTP_1_1

  val zookeeperServiceName: String = "output-zookeeper-test-service"
  val testNamespace = "test_namespace"

  val esIndex: String = "test_index_for_output_engine"
  val databaseName: String = "test_database_for_output_engine"
  val restBasePath = "/test/base_path/for/output_engine"

  val streamService = ConnectionRepository.getStreamRepository
  val serviceManager = ConnectionRepository.getServiceRepository
  val providerService = ConnectionRepository.getProviderRepository
  val instanceService = ConnectionRepository.getInstanceRepository
  val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  val configService: GenericMongoRepository[ConfigurationSettingDomain] = ConnectionRepository.getConfigRepository

  val esInstanceName: String = "test-es-instance-for-output-engine"
  val jdbcInstanceName: String = "test-jdbc-instance-for-output-engine"
  val restInstanceName: String = "test-rest-instance-for-output-engine"

  val tstreamPartitions: Int = 4
  val tstreamTtl: Long = 60000l
  val producerPort: Int = 8030

  val inputData = Array(
    "abc",
    "a'b",
    """a"b""",
    """a\b""",
    """a
      |b
    """.stripMargin)

  val objectSerializer = new ObjectSerializer()
  private val serializer = new JsonSerializer

  private val esProviderHosts = config.getString(OutputBenchmarkConfigNames.esHosts).split(",").map(host => host.trim)
  private val jdbcHosts = config.getString(OutputBenchmarkConfigNames.jdbcHosts).split(",").map(host => host.trim)
  private val restHosts = config.getString(OutputBenchmarkConfigNames.restHosts).split(",").map(host => host.trim)
  private val zookeeperHosts = config.getString(BenchmarkConfigNames.zkHosts).split(",").map(host => host.trim)
  private val zookeeperProvider = new ProviderDomain(zookeeperProviderName, zookeeperProviderName,
    zookeeperHosts, "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamServiceDomain(tstreamServiceName, tstreamServiceName, zookeeperProvider, TestStorageServer.prefix, TestStorageServer.token)
  private val tstreamFactory = new TStreamsFactory()

  setTStreamFactoryProperties()

  val storageClient = tstreamFactory.getStorageClient()

  private def setTStreamFactoryProperties() = {
    setAuthOptions(tstrqService)
    setCoordinationOptions(tstrqService)
    setBindHostForAgents()
  }

  private def setAuthOptions(tStreamService: TStreamServiceDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.Common.authenticationKey, tStreamService.token)
  }

  private def setCoordinationOptions(tStreamService: TStreamServiceDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.endpoints, tStreamService.provider.getConcatenatedHosts())
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.path, tStreamService.prefix)
  }

  private def setBindHostForAgents() = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindHost, agentsHost)
    tstreamFactory.setProperty(ConfigurationOptions.Consumer.Subscriber.bindHost, agentsHost)
  }

  def createData(countTxns: Int, countElements: Int) = {
    val tStream: TStreamStreamDomain = new TStreamStreamDomain(
      tstreamInputName,
      tstrqService,
      tstreamPartitions)

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
    (0 until countTxns) foreach { (x: Int) =>
      val transaction = producer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened)
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        val string = inputData(number % inputData.length)
        println(s"write data $number, |$string|")
        val msg = objectSerializer.serialize((number, string).asInstanceOf[Object])
        transaction.send(msg)
      }
      println("checkpoint")
      transaction.checkpoint()
    }
  }

  def openEsConnection(outputStream: StreamDomain) = {
    val esService: ESServiceDomain = outputStream.service.asInstanceOf[ESServiceDomain]
    val hosts = esService.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)

    (client, esService)
  }

  def openJdbcConnection(outputStream: StreamDomain) = {
    val jdbcService: JDBCServiceDomain = outputStream.service.asInstanceOf[JDBCServiceDomain]
    val client = JdbcClientBuilder.
      setHosts(jdbcService.provider.hosts).
      setDriver(jdbcDriver).
      setUsername(jdbcService.provider.login).
      setPassword(jdbcService.provider.password).
      setDatabase(jdbcService.database).
      setTable(outputStream.name).
      build()

    client
  }

  def deleteIndex() = {
    if (streamService.get(esStreamName).isDefined) {
      val stream = streamService.get(esStreamName).get.asInstanceOf[ESStreamDomain]
      val (client, _) = openEsConnection(stream)
      client.deleteIndex(esIndex)

      client.close()
    }
  }

  def clearDatabase() = {
    if (streamService.get(jdbcStreamName).isDefined) {
      val stream = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCStreamDomain]
      val client = openJdbcConnection(stream)
      client.start()
      val sql = s"DROP TABLE $jdbcStreamName"
      client.execute(sql)
      client.close()
    }
  }

  def createProducer(stream: TStreamStreamDomain) = {
    setProducerBindPort()
    setStreamOptions(stream)

    tstreamFactory.getProducer(
      "producer for " + stream.name,
      (0 until stream.partitions).toSet)
  }

  private def setProducerBindPort() = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindPort, producerPort)
  }

  def createConsumer(stream: TStreamStreamDomain): consumer.Consumer = {
    setStreamOptions(stream)

    tstreamFactory.getConsumer(
      stream.name,
      (0 until stream.partitions).toSet,
      Oldest)
  }

  protected def setStreamOptions(stream: TStreamStreamDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, stream.name)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, stream.partitions)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.description, stream.description)
  }

  def create_table: String = {
    s"CREATE TABLE $jdbcStreamName " +
      "(id VARCHAR(255) not NULL, " +
      " value INTEGER, " +
      " string_value VARCHAR(255), " +
      " txn BIGINT, " + //use NUMBER(19) for oracle
      " PRIMARY KEY ( id ))"
  }

  def close() = {
    tstreamFactory.close()
  }


  def createProviders() = {
    val esProvider = new ProviderDomain(esProviderName, "", esProviderHosts, "", "", ProviderLiterals.elasticsearchType)
    providerService.save(esProvider)

    providerService.save(zookeeperProvider)

    val jdbcProvider = new JDBCProviderDomain(jdbcProviderName, "", jdbcHosts, "admin", "admin", jdbcDriver)
    providerService.save(jdbcProvider)

    val restProvider = new ProviderDomain(restProviderName, "", restHosts, "", "", ProviderLiterals.restType)
    providerService.save(restProvider)
  }

  def createServices() = {
    val esProv: ProviderDomain = providerService.get(esProviderName).get
    val esService: ESServiceDomain = new ESServiceDomain(esServiceName, esServiceName, esProv, esIndex)
    serviceManager.save(esService)

    serviceManager.save(tstrqService)

    val zkService = new ZKServiceDomain(zookeeperServiceName, zookeeperServiceName, zookeeperProvider, testNamespace)
    serviceManager.save(zkService)

    val jdbcProvider = providerService.get(jdbcProviderName).get.asInstanceOf[JDBCProviderDomain]
    val jdbcService = new JDBCServiceDomain(jdbcServiceName, jdbcServiceName, jdbcProvider, databaseName)
    serviceManager.save(jdbcService)

    val restProvider = providerService.get(restProviderName).get
    val restService = new RestServiceDomain(restServiceName, restServiceName, restProvider, restBasePath, restHttpVersion, restHeaders)
    serviceManager.save(restService)
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
    val stream = streamService.get(esStreamName).get.asInstanceOf[ESStreamDomain]
    val esClient = openEsConnection(stream)
    esClient._1.createIndex(esIndex)
    createIndexMapping(mapping)
  }

  def createIndexMapping(mapping: XContentBuilder) = {
    val stream = streamService.get(esStreamName).get.asInstanceOf[ESStreamDomain]
    val esClient = openEsConnection(stream)
    esClient._1.createMapping(esIndex, esStreamName, mapping)
  }

  def createTable() = {
    val stream = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCStreamDomain]
    val client = openJdbcConnection(stream)
    client.start()
    client.execute(create_table)
  }

  def createStreams(partitions: Int) = {
    val esService = serviceManager.get(esServiceName).get.asInstanceOf[ESServiceDomain]
    val esStream: ESStreamDomain = new ESStreamDomain(esStreamName, esService)
    streamService.save(esStream)

    val tService: TStreamServiceDomain = serviceManager.get(tstreamServiceName).get.asInstanceOf[TStreamServiceDomain]
    val tStream: TStreamStreamDomain = new TStreamStreamDomain(tstreamInputName, tService, partitions)
    streamService.save(tStream)

    val jdbcService: JDBCServiceDomain = serviceManager.get(jdbcServiceName).get.asInstanceOf[JDBCServiceDomain]
    val jdbcStream: JDBCStreamDomain = new JDBCStreamDomain(jdbcStreamName, jdbcService, "test")
    streamService.save(jdbcStream)

    val restService = serviceManager.get(restServiceName).get.asInstanceOf[RestServiceDomain]
    val restStream = new RestStreamDomain(restStreamName, restService)
    streamService.save(restStream)

    storageClient.createStream(
      tstreamInputName,
      partitions,
      tstreamTtl,
      tstreamInputName)
  }

  def createInstance(instanceName: String, checkpointMode: String, checkpointInterval: Long,
                     streamName: String, moduleName: String) = {

    val task1 = new Task()
    task1.inputs = Map(tstreamInputName -> Array(0, tstreamPartitions - 1)).asJava
    val executionPlan = new ExecutionPlan(Map(instanceName + "-task0" -> task1).asJava)

    val instance = new OutputInstance(
      name = instanceName,
      moduleType = EngineLiterals.outputStreamingType,
      moduleName = moduleName,
      moduleVersion = "1.0",
      engine = "com.bwsw.output.streaming.engine-1.0",
      coordinationService = zookeeperServiceName,
      status = EngineLiterals.started,
      description = "some description of test instance",
      input = tstreamInputName,
      output = streamName,
      checkpointInterval = checkpointInterval,
      options = """{"hey": "hey"}""",
      startFrom = EngineLiterals.oldestStartMode,
      executionPlan = executionPlan,
      checkpointMode = checkpointMode)


    instanceService.save(instance.to)
  }

  def deleteInstance(instanceName: String) = {
    instanceService.delete(instanceName)
  }

  def deleteStreams() = {
    streamService.delete(esStreamName)
    streamService.delete(tstreamInputName)
    streamService.delete(jdbcStreamName)
    streamService.delete(restStreamName)

    storageClient.deleteStream(esStreamName)
    storageClient.deleteStream(tstreamInputName)
    storageClient.deleteStream(jdbcStreamName)
    storageClient.deleteStream(restStreamName)
  }

  def deleteServices() = {
    serviceManager.delete(esServiceName)
    serviceManager.delete(tstreamServiceName)
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(jdbcServiceName)
    serviceManager.delete(restServiceName)
  }

  def deleteProviders() = {
    providerService.delete(zookeeperProviderName)
    providerService.delete(esProviderName)
    providerService.delete(jdbcProviderName)
    providerService.delete(restProviderName)
  }

  def uploadModule(moduleJar: File) = {
    val builder = new StringBuilder
    val jar = new JarFile(moduleJar)
    val enu = jar.entries()
    while (enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        val result = Try {
          var line = reader.readLine
          while (Option(line).isDefined) {
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