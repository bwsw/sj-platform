package com.bwsw.sj.common.module

import java.io.{InputStreamReader, BufferedReader, File}
import java.net.InetSocketAddress
import java.util.jar.JarFile

import com.aerospike.client.Host
import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.utils.CassandraHelper._
import com.bwsw.tstreams.coordination.Coordinator
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.services.BasicStreamService
import com.datastax.driver.core.Cluster
import org.redisson.{Config, Redisson}

object DataFactory {

  private val aerospikeHosts: Array[String] = System.getenv("AEROSPIKE_HOSTS").split(",")
  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",")
  private val redisHosts = System.getenv("REDIS_HOSTS").split(",")
  private val testNamespace = "test"
  private val serializer = new JsonSerializer()
  private val cluster = Cluster.builder().addContactPoint(cassandraHost).build()
  private val session = cluster.connect()

  private val hosts = aerospikeHosts.map(x => {
    val hostPort = x.split(":")
    new Host(hostPort(0), hostPort(1).toInt)
  }).toList
  private lazy val aerospikeOptions = new AerospikeStorageOptions(testNamespace, hosts)
  private lazy val aerospikeStorageFactory = new AerospikeStorageFactory()
  private lazy val dataStorage = aerospikeStorageFactory.getInstance(aerospikeOptions)

  private lazy val metadataStorageFactory: MetadataStorageFactory = new MetadataStorageFactory()
  private lazy val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(
    cassandraHosts = List(new InetSocketAddress(cassandraHost, cassandraPort)),
    keyspace = cassandraTestKeyspace)

  private val config = new Config()
  config.useSingleServer().setAddress(redisHosts(0))
  private lazy val lockService = new Coordinator(testNamespace, Redisson.create(config))

  def cassandraSetup() = {
    createKeyspace(session, cassandraTestKeyspace)
    createMetadataTables(session, cassandraTestKeyspace)
  }

  def close() = {
    aerospikeStorageFactory.closeFactory()
    metadataStorageFactory.closeFactory()
    session.execute(s"DROP KEYSPACE $cassandraTestKeyspace")
    session.close()
    cluster.close()
  }

  def createProviders(providerService: GenericMongoService[Provider]) = {
    val cassandraProvider = new Provider
    cassandraProvider.providerType = "cassandra"
    cassandraProvider.name = "cassandra test provider"
    cassandraProvider.login = ""
    cassandraProvider.password = ""
    cassandraProvider.description = "cassandra provider"
    cassandraProvider.hosts = Array(s"$cassandraHost:9042")
    providerService.save(cassandraProvider)

    val aerospikeProvider = new Provider
    aerospikeProvider.providerType = "aerospike"
    aerospikeProvider.name = "aerospike test provider"
    aerospikeProvider.login = ""
    aerospikeProvider.password = ""
    aerospikeProvider.description = "aerospike provider"
    aerospikeProvider.hosts = aerospikeHosts
    providerService.save(aerospikeProvider)

    val redisProvider = new Provider
    redisProvider.providerType = "redis"
    redisProvider.name = "redis test provider"
    redisProvider.login = ""
    redisProvider.password = ""
    redisProvider.description = "redis provider"
    redisProvider.hosts = redisHosts
    providerService.save(redisProvider)

    val zookeeperProvider = new Provider
    zookeeperProvider.providerType = "zookeeper"
    zookeeperProvider.name = "zookeeper test provider"
    zookeeperProvider.login = ""
    zookeeperProvider.password = ""
    zookeeperProvider.description = "zookeeper provider"
    zookeeperProvider.hosts = zookeeperHosts
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete("cassandra test provider")
    providerService.delete("aerospike test provider")
    providerService.delete("redis test provider")
    providerService.delete("zookeeper test provider")
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val cassService = new CassandraService
    val cassProv = providerService.get("cassandra test provider")
    cassService.keyspace = cassandraTestKeyspace
    cassService.name = "cassandra test service"
    cassService.description = "cassandra test service"
    cassService.provider = cassProv
    cassService.serviceType = "CassDB"
    serviceManager.save(cassService)

    val aeroService = new AerospikeService
    val aeroProv = providerService.get("aerospike test provider")
    aeroService.namespace = testNamespace
    aeroService.name = "aerospike test service"
    aeroService.description = "aerospike test service"
    aeroService.provider = aeroProv
    aeroService.serviceType = "ArspkDB"
    serviceManager.save(aeroService)

    val redisService = new RedisService
    val redisProv = providerService.get("redis test provider")
    redisService.namespace = testNamespace
    redisService.name = "redis test service"
    redisService.description = "redis test service"
    redisService.provider = redisProv
    redisService.serviceType = "RdsCoord"
    serviceManager.save(redisService)

    val tstrqService = new TStreamService
    tstrqService.name = "tstream test service"
    tstrqService.metadataProvider = cassProv
    tstrqService.metadataNamespace = cassandraTestKeyspace
    tstrqService.dataProvider = aeroProv
    tstrqService.dataNamespace = testNamespace
    tstrqService.lockProvider = redisProv
    tstrqService.lockNamespace = testNamespace
    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoService[Service]) = {
    serviceManager.delete("cassandra test service")
    serviceManager.delete("aerospike test service")
    serviceManager.delete("redis test service")
    serviceManager.delete("tstream test service")
  }

  def createStreams(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service]) = {
    val localGenerator = new Generator
    localGenerator.generatorType = "local"

    val tService = serviceManager.get("tstream test service")

    val s1 = new SjStream
    s1.name = "test_tstream1"
    s1.description = "test_tstream1"
    s1.partitions = 1
    s1.service = tService
    s1.tags = "some tags"
    s1.generator = localGenerator
    sjStreamService.save(s1)

    val s2 = new SjStream
    s2.name = "test_tstream2"
    s2.description = "test_tstream2"
    s2.partitions = 1
    s2.service = tService
    s2.tags = "some tags"
    s2.generator = localGenerator
    sjStreamService.save(s2)
  }

  def deleteStreams(streamService: GenericMongoService[SjStream]) = {
    streamService.delete("test_tstream1")
    streamService.delete("test_tstream2")
  }

  def createTStreams() = {
    BasicStreamService.createStream("test_tstream1", 1, 1000 * 60, "description of test tstream1", metadataStorage, dataStorage, lockService)
    BasicStreamService.createStream("test_tstream2", 1, 1000 * 60, "description of test tstream2", metadataStorage, dataStorage, lockService)
  }

  def deleteTStreams() = {
    BasicStreamService.deleteStream("test_tstream1", metadataStorage)
    BasicStreamService.deleteStream("test_tstream2", metadataStorage)
  }

  def createInstance(instanceService: GenericMongoService[RegularInstance]) = {
    import scala.collection.JavaConverters._

    val instance = new RegularInstance()
    instance.name = "test instance"
    instance.moduleType = "regular-streaming"
    instance.moduleName = "test stub"
    instance.moduleVersion = "0.1"
    instance.status = "started"
    instance.description = "some description of test instance"
    instance.inputs = Array("test_tstream1/split")
    instance.outputs = Array("test_tstream2")
    instance.checkpointMode = "every-nth"
    instance.checkpointInterval = 1
    instance.stateManagement = "none"
    instance.stateFullCheckpoint = 0
    instance.parallelism = 0
    instance.options = ""
    instance.startFrom = "oldest"
    instance.perTaskCores = 0
    instance.perTaskRam = 0
    instance.options = null

    val executionPlan = new ExecutionPlan()
    val task = new Task()
    task.inputs = Map(("test_tstream1", Array(0, 0))).asJava
    executionPlan.tasks = Map(("test instance-for-task0", task)).asJava
    instance.executionPlan = executionPlan

    instance.idle = 10

    instanceService.save(instance)
  }

  def deleteInstance(instanceService: GenericMongoService[RegularInstance]) = {
    instanceService.delete("test instance")
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
}
