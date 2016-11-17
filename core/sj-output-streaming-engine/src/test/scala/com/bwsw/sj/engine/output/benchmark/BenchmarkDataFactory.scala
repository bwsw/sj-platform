package com.bwsw.sj.engine.output.benchmark

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.common.traits.Serializer
import com.bwsw.common._
import com.bwsw.sj.common.DAL.model.{ESService, Generator, _}
import com.bwsw.sj.common.DAL.model.module.{OutputInstance, Task}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities.module.ExecutionPlan
import com.bwsw.sj.common.utils.{GeneratorLiterals, ProviderLiterals, ServiceLiterals, _}
import com.bwsw.sj.engine.core.entities.JdbcEnvelope
import com.bwsw.tstreams.agents.consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}
import com.bwsw.tstreams.generator.LocalTransactionGenerator
import com.bwsw.tstreams.services.BasicStreamService
import org.elasticsearch.search.SearchHits

import scala.collection.JavaConverters._

/**
 *
 *
 * @author Kseniya Tomskikh
 */
object BenchmarkDataFactory {


  private var txnFieldForJdbc: String = new JdbcEnvelope().getTxnName
  val metadataProviderName: String = "test-metprov-1"
  val cassandraTestKeyspace: String = "bench"
  val dataProviderName: String = "test-dataprov-1"
  val lockProviderName: String = "test-lockprov-1"
  val lockNamespace: String = "bench"
  val tServiceName: String = "test-tserv-1"
  val tStreamName: String = "test-tstr-1"

  val esProviderName: String = "test-esprov-1"
  val esServiceName: String = "test-esserv-1"
  val esStreamName: String = "test-es-1"

  val jdbcProviderName: String = "test-jdbcprov-1"
  val jdbcServiceName: String = "test-jdbcserv-1"
  val jdbcStreamName: String = "testjdbcstr1"
  val jdbcDriver: String = "postgresql"

  val zkServiceName: String = "test-zkserv-1"

  val indexName: String = "bench"
  val databaseName: String = "test"

  val streamService = ConnectionRepository.getStreamService
  val serviceManager = ConnectionRepository.getServiceManager
  val providerService = ConnectionRepository.getProviderService
  val instanceService = ConnectionRepository.getInstanceService
  val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  val configService: GenericMongoService[ConfigurationSetting] = ConnectionRepository.getConfigService

  val objectSerializer = new ObjectSerializer()
  private val serializer: Serializer = new JsonSerializer

  private val esProviderHosts = System.getenv("ES_HOSTS").split(",").map(host => host.trim)
  private val cassandraHosts = System.getenv("CASSANDRA_HOSTS").split(",").map(host => host.trim)
  private val jdbcHosts = System.getenv("JDBC_HOSTS").split(",").map(host => host.trim)
  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",").map(host => host.trim)

  private val cassandraFactory = new CassandraFactory()

  private val cassandraProvider = new Provider("cassandra-test-provider", "cassandra provider",
    cassandraHosts, "", "", ProviderLiterals.cassandraType)
  private val zookeeperProvider = new Provider("zookeeper-test-provider", "zookeeper provider",
    zookeeperHosts, "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamService("test-tserv-1", ServiceLiterals.tstreamsType, "tstream test service",
    cassandraProvider, cassandraTestKeyspace, cassandraProvider, cassandraTestKeyspace, zookeeperProvider, "unit")
  private val tstreamFactory = new TStreamsFactory()



  setTStreamFactoryProperties()

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
        tstreamFactory.setProperty(
          TSF_Dictionary.Data.Cluster.DRIVER,
          TSF_Dictionary.Data.Cluster.Consts.DATA_DRIVER_AEROSPIKE
        )
      case _ =>
        tstreamFactory.setProperty(
          TSF_Dictionary.Data.Cluster.DRIVER,
          TSF_Dictionary.Data.Cluster.Consts.DATA_DRIVER_CASSANDRA
        )
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
    val tStream: TStreamSjStream =  new TStreamSjStream (
      tStreamName, "", 4, tstrqService,
      "stream.t-stream", Array("tag"), new Generator()
    )

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
      setTxnField(txnFieldForJdbc).
      build()

    (client, jdbcService)
  }

  def clearEsStream() = {
    if (streamService.get(esStreamName).isDefined) {
      val stream = streamService.get(esStreamName).get.asInstanceOf[ESSjStream]
      val (client, service) = openEsConnection(stream)
      var outputData: Option[SearchHits] = None
      try {
        outputData = Some(client.search(service.index, stream.name))
      }
      catch {
        case _: Exception =>
      }

      if (outputData.isDefined) {
        outputData.get.getHits.foreach { hit =>
          val id = hit.getId
          client.deleteDocumentByTypeAndId(service.index, stream.name, id)
        }
        client.deleteIndex(indexName)
      }
      client.close()
    }
  }

  def clearDatabase() = {
    if (streamService.get(jdbcStreamName).isDefined) {
      val stream = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCSjStream]
      val client = openJdbcConnection(stream)
      val sql = s"DROP TABLE $jdbcStreamName"
      client._1.execute(sql)
      client._1.close()
    }
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

  def open() = cassandraFactory.open(splitHosts(cassandraHosts))

  def prepareCassandra() = {
    cassandraFactory.createKeyspace(cassandraTestKeyspace)
    cassandraFactory.createMetadataTables(cassandraTestKeyspace)
    cassandraFactory.createDataTable(cassandraTestKeyspace)
  }

  def create_table: String = {
    s"CREATE TABLE $jdbcStreamName " +
    "(id VARCHAR(255) not NULL, " +
    " value INTEGER, " +
    " txn VARCHAR(255), " +
    " PRIMARY KEY ( id ))"
  }

  def close() = {
    cassandraFactory.close()
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

    val metadataProvider = new Provider()
    metadataProvider.name = metadataProviderName
    metadataProvider.hosts = cassandraHosts
    metadataProvider.providerType = ProviderLiterals.cassandraType
    metadataProvider.login = ""
    metadataProvider.password = ""
    providerService.save(metadataProvider)

    val dataProvider = new Provider()
    dataProvider.name = dataProviderName
    dataProvider.hosts = cassandraHosts
    dataProvider.providerType = ProviderLiterals.cassandraType
    dataProvider.login = ""
    dataProvider.password = ""
    providerService.save(dataProvider)

    val lockProvider = new Provider()
    lockProvider.name = lockProviderName
    lockProvider.hosts = zookeeperHosts
    lockProvider.providerType = ProviderLiterals.zookeeperType
    lockProvider.login = ""
    lockProvider.password = ""
    providerService.save(lockProvider)

    val jdbcProvider = new Provider()
    jdbcProvider.name = jdbcProviderName
    jdbcProvider.hosts = jdbcHosts
    jdbcProvider.providerType = ProviderLiterals.jdbcType
    jdbcProvider.login = "root"
    jdbcProvider.password = "root"
    providerService.save(jdbcProvider)
  }

  def createServices() = {

    val esProv: Provider = providerService.get(esProviderName).get
    val esService: ESService = new ESService()
    esService.name = esServiceName
    esService.serviceType = ServiceLiterals.elasticsearchType
    esService.description = "es service for benchmarks"
    esService.provider = esProv
    esService.index = indexName
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
    tStreamService.metadataNamespace = cassandraTestKeyspace
    tStreamService.dataProvider = dataProvider
    tStreamService.dataNamespace = cassandraTestKeyspace
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

    val jdbcProvider: Provider = providerService.get(jdbcProviderName).get
    val jdbcService = new JDBCService()
    jdbcService.name = jdbcServiceName
    jdbcService.driver = jdbcDriver
    jdbcService.description = "jdbc service for benchmark"
    jdbcService.provider = jdbcProvider
    jdbcService.database = databaseName
    serviceManager.save(jdbcService)
  }

  def createIndex() = {
    val stream = streamService.get(esStreamName).get.asInstanceOf[ESSjStream]
    val esClient = openEsConnection(stream)
    esClient._1.createIndex(indexName)
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
    esStream.streamType = "elasticsearch-output"
    esStream.service = esService
    esStream.tags = Array("tag1")
    streamService.save(esStream)

    val tService: TStreamService = serviceManager.get(tServiceName).get.asInstanceOf[TStreamService]
    val tStream: TStreamSjStream = new TStreamSjStream()
    tStream.name = tStreamName
    tStream.description = "t-stream for benchmarks"
    tStream.streamType = "stream.t-stream"
    tStream.service = tService
    tStream.tags = Array("tag1")
    tStream.partitions = partitions
    tStream.generator = new Generator(GeneratorLiterals.localType)
    streamService.save(tStream)

    val jdbcService: JDBCService = serviceManager.get(jdbcServiceName).get.asInstanceOf[JDBCService]
    val jdbcStream: JDBCSjStream = new JDBCSjStream()
    jdbcStream.name = jdbcStreamName
    jdbcStream.primary = "id"
    jdbcStream.description = "jdbc stream for benchmarks"
    jdbcStream.streamType = "jdbc-output"
    jdbcStream.service = jdbcService
    jdbcStream.tags = Array("tag1")
    streamService.save(jdbcStream)

    val metadataHosts = splitHosts(tService.metadataProvider.hosts)
    val cassandraFactory = new CassandraFactory()
    cassandraFactory.open(metadataHosts)
    val metadataStorage = cassandraFactory.getMetadataStorage(tService.metadataNamespace)

    val namespace = tService.dataNamespace
    val dataStorage = cassandraFactory.getDataStorage(namespace)

    BasicStreamService.createStream(tStreamName,
      partitions,
      60000,
      "", metadataStorage,
      dataStorage)

    cassandraFactory.close()
  }

  def createInstance(instanceName: String, checkpointMode: String, checkpointInterval: Long,
                     streamName: String, moduleName: String) = {

    val task1 = new Task()
    task1.inputs = Map(tStreamName -> Array(0, 3)).asJava
    val executionPlan = new ExecutionPlan(Map((instanceName + "-task0", task1)).asJava)

    val instance = new OutputInstance()
    instance.name = instanceName
    instance.moduleType = EngineLiterals.outputStreamingType
    instance.moduleName = moduleName
    instance.moduleVersion = "1.0"
    instance.status = EngineLiterals.started
    instance.description = "some description of test instance"
    instance.inputs = Array(tStreamName)
    instance.outputs = Array(streamName)
    instance.checkpointMode = checkpointMode
    instance.checkpointInterval = checkpointInterval
    instance.parallelism = 1
    instance.options = """{"hey": "hey"}"""
    instance.startFrom = EngineLiterals.oldestStartMode
    instance.perTaskCores = 0.1
    instance.perTaskRam = 64
    instance.performanceReportingInterval = 10000
    instance.executionPlan = executionPlan
    instance.engine = "com.bwsw.output.streaming.engine-1.0"
    instance.coordinationService = serviceManager.get(zkServiceName).get.asInstanceOf[ZKService]

    instanceService.save(instance)
  }

  def deleteInstance(instanceName: String) = {
    instanceService.delete(instanceName)
  }

  def deleteStreams() = {
    streamService.delete(esStreamName)
    streamService.delete(tStreamName)
    streamService.delete(jdbcStreamName)
  }

  def deleteServices() = {
    serviceManager.delete(esServiceName)
    serviceManager.delete(tServiceName)
    serviceManager.delete(zkServiceName)
    serviceManager.delete(jdbcServiceName)
  }

  def deleteProviders() = {
    providerService.delete(dataProviderName)
    providerService.delete(metadataProviderName)
    providerService.delete(lockProviderName)
    providerService.delete(esProviderName)
    providerService.delete(jdbcProviderName)
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

