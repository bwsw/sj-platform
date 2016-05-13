package com.bwsw.sj.common.module

import java.io.{BufferedReader, File, InputStreamReader}
import java.net.InetSocketAddress
import java.util.jar.JarFile

import com.aerospike.client.Host
import com.bwsw.common.{ObjectSerializer, JsonSerializer}
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.utils.CassandraHelper._
import com.bwsw.tstreams.agents.consumer.Offsets.Oldest
import com.bwsw.tstreams.agents.consumer.{BasicConsumerOptions, BasicConsumer}
import com.bwsw.tstreams.agents.producer.InsertionType.BatchInsert
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerOptions, ProducerPolicies}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.coordination.Coordinator
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.BasicStream
import com.datastax.driver.core.Cluster
import org.redisson.{Config, Redisson}

object DataFactory {

  private val aerospikeHosts: Array[String] = System.getenv("AEROSPIKE_HOSTS").split(",")
  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",")
  private val redisHosts = System.getenv("REDIS_HOSTS").split(",")
  private val testNamespace = "test"
  private val serializer = new JsonSerializer()
  private val objectSerializer = new ObjectSerializer()
  private val cluster = Cluster.builder().addContactPoint(cassandraHost).build()
  private val session = cluster.connect()

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

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
  private lazy val redissonClient = Redisson.create(config)
  private lazy val lockService = new Coordinator(testNamespace, redissonClient)

  def cassandraSetup() = {
    createKeyspace(session, cassandraTestKeyspace)
    createMetadataTables(session, cassandraTestKeyspace)
  }

  def cassandraDestroy() = {
    session.execute(s"DROP KEYSPACE $cassandraTestKeyspace")
  }

  def close() = {
    aerospikeStorageFactory.closeFactory()
    metadataStorageFactory.closeFactory()
    redissonClient.shutdown()
    session.close()
    cluster.close()
  }

  def createProviders(providerService: GenericMongoService[Provider]) = {
    val cassandraProvider = new Provider("cassandra test provider", "cassandra provider", Array(s"$cassandraHost:9042"), "", "", "cassandra")
    providerService.save(cassandraProvider)

    val aerospikeProvider = new Provider("aerospike test provider", "aerospike provider", aerospikeHosts, "", "", "aerospike")
    providerService.save(aerospikeProvider)

    val redisProvider = new Provider("redis test provider", "redis provider", redisHosts, "", "", "redis")
    providerService.save(redisProvider)

    val zookeeperProvider = new Provider("zookeeper test provider", "zookeeper provider", zookeeperHosts, "", "", "zookeeper")
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete("cassandra test provider")
    providerService.delete("aerospike test provider")
    providerService.delete("redis test provider")
    providerService.delete("zookeeper test provider")
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val cassProv = providerService.get("cassandra test provider")
    val cassService = new CassandraService("cassandra test service", "CassDB", "cassandra test service", cassProv, cassandraTestKeyspace)
    serviceManager.save(cassService)

    val aeroProv = providerService.get("aerospike test provider")
    val aeroService = new AerospikeService("aerospike test service", "ArspkDB", "aerospike test service", aeroProv, testNamespace)
    serviceManager.save(aeroService)

    val redisProv = providerService.get("redis test provider")
    val redisService = new RedisService("redis test service", "RdsCoord", "redis test service", redisProv, testNamespace)
    serviceManager.save(redisService)

    val tstrqService = new TStreamService("tstream test service", "TstrQ", "tstream test service",
      cassProv, cassandraTestKeyspace, aeroProv, testNamespace, redisProv, testNamespace)
    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoService[Service]) = {
    serviceManager.delete("cassandra test service")
    serviceManager.delete("aerospike test service")
    serviceManager.delete("redis test service")
    serviceManager.delete("tstream test service")
  }

  def createStreams(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service]) = {
    val localGenerator = new Generator("local")

    val tService = serviceManager.get("tstream test service")

    val s1 = new SjStream("test_tstream1", "test_tstream1", 1, tService, "Tstream", "some tags", localGenerator)
    sjStreamService.save(s1)

    val s2 = new SjStream("test_tstream2", "test_tstream2", 1, tService, "Tstream", "some tags", localGenerator)
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
    instance.options = """{"hey": "hey"}"""
    instance.startFrom = "oldest"
    instance.perTaskCores = 0
    instance.perTaskRam = 0

    val task = new Task(Map(("test_tstream1", Array(0, 0))).asJava)
    instance.executionPlan = new ExecutionPlan(Map(("test instance-task0", task)).asJava)

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

  def createData(countTxns: Int, countElements: Int, streamService: GenericMongoService[SjStream]) = {
    val producer = createProducer(streamService.get("test_tstream1"))
    var number = 0
    val s = System.nanoTime
    (0 until countTxns) foreach { (x: Int) =>
      val txn = producer.newTransaction(ProducerPolicies.errorIfOpen)
      (0 until countElements) foreach { (y: Int) =>
        number += 1
        txn.send(objectSerializer.serialize(number.asInstanceOf[Object]))
      }
      txn.checkpoint()
    }

    println(s"producer time: ${(System.nanoTime - s) / 1000000}")
  }

  def createInputConsumer(streamService: GenericMongoService[SjStream]) = {
    createConsumer("test_tstream1", streamService)
  }

  def createOutputConsumer(streamService: GenericMongoService[SjStream]) = {
    createConsumer("test_tstream2", streamService)
  }

  private def createProducer(stream: SjStream) = {
    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage, lockService)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (0 until stream.partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new BasicProducerOptions[Array[Byte], Array[Byte]](
      transactionTTL = 6,
      transactionKeepAliveInterval = 2,
      producerKeepAliveInterval = 1,
      roundRobinPolicy,
      BatchInsert(5),
      timeUuidGenerator,
      converter)

    new BasicProducer[Array[Byte], Array[Byte]]("producer for " + basicStream.name, basicStream, options)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoService[SjStream]): BasicConsumer[Array[Byte], Array[Byte]] = {
    val stream = streamService.get(streamName)

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage, lockService)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (0 until stream.partitions).toList)

    val timeUuidGenerator = new LocalTimeUUIDGenerator

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      Oldest,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicConsumer[Array[Byte], Array[Byte]](basicStream.name, basicStream, options)
  }
}
