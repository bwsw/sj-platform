package com.bwsw.sj.engine.input

import java.io.{PrintStream, BufferedReader, File, InputStreamReader}
import java.net.{Socket, InetSocketAddress}
import java.util
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask, Instance}
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.engine.core.utils.CassandraHelper
import com.bwsw.sj.engine.core.utils.CassandraHelper._
import com.bwsw.tstreams.agents.consumer.Offsets.Oldest
import com.bwsw.tstreams.agents.consumer.{Consumer, ConsumerOptions}
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.data.cassandra.{CassandraStorageOptions, CassandraStorageFactory}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.TStream
import com.datastax.driver.core.Cluster

object DataFactory {

  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS").split(",")
  private val testNamespace = "test"
  private val instanceName = "test-instance-for-input-engine"
  private var instanceOutputs: Array[String] = Array()
  private val tasks = new util.HashMap[String, InputTask]()
  private val host = "localhost"
  private val port = 8888
  tasks.put(s"$instanceName-task0", new InputTask(host, port))
  private val partitions = 1
  private val serializer = new JsonSerializer()
  private val cluster = Cluster.builder().addContactPoint(cassandraHost).build()
  private val session = cluster.connect()
  private val dataStorageFactory = new CassandraStorageFactory()
  private val dataStorageOptions = new CassandraStorageOptions(
    List(new InetSocketAddress(CassandraHelper.cassandraHost, CassandraHelper.cassandraPort)),
    cassandraTestKeyspace
  )
  val outputCount = 2

  def writeData(totalInputElements: Int, totalDuplicateElements: Int) = {
    try {
      val socket = new Socket(host, port)
      var amountOfDuplicates = -1
      var amountOfElements = 0
      var currentElement = 1
      val out = new PrintStream(socket.getOutputStream)

      while (amountOfElements < totalInputElements) {
        if (amountOfDuplicates != totalDuplicateElements) {
          out.println(currentElement)
          out.flush()
          amountOfElements += 1
          amountOfDuplicates += 1
        }
        else {
          currentElement += 1
          out.println(currentElement)
          out.flush()
          amountOfElements += 1
        }
      }

      socket.close()
    }
    catch {
      case e: Exception =>
        System.out.println("init error: " + e)
    }
  }

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = obj
  }

  private lazy val metadataStorageFactory: MetadataStorageFactory = new MetadataStorageFactory()
  private lazy val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(
    cassandraHosts = List(new InetSocketAddress(cassandraHost, cassandraPort)),
    keyspace = cassandraTestKeyspace)

  def cassandraSetup() = {
    createKeyspace(session, cassandraTestKeyspace)
    createMetadataTables(session, cassandraTestKeyspace)
  }

  def cassandraDestroy() = {
    session.execute(s"DROP KEYSPACE $cassandraTestKeyspace")
  }

  def close() = {
    dataStorageFactory.closeFactory()
    metadataStorageFactory.closeFactory()
    session.close()
    cluster.close()
  }

  def createProviders(providerService: GenericMongoService[Provider]) = {
    val cassandraProvider = new Provider("cassandra_test_provider", "cassandra provider", Array(s"$cassandraHost:9042"), "", "", "cassandra")
    providerService.save(cassandraProvider)

    val zookeeperProvider = new Provider("zookeeper_test_provider", "zookeeper provider", zookeeperHosts, "", "", "zookeeper")
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete("cassandra_test_provider")
    providerService.delete("aerospike_test_provider")
    providerService.delete("zookeeper_test_provider")
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val cassProv = providerService.get("cassandra_test_provider")
    val cassService = new CassandraService("cassandra_test_service", "CassDB", "cassandra test service", cassProv, cassandraTestKeyspace)
    serviceManager.save(cassService)

    val zkProv = providerService.get("zookeeper_test_provider")
    val zkService = new ZKService("zookeeper_test_service", "ZKCoord", "zookeeper test service", zkProv, testNamespace)
    serviceManager.save(zkService)

    val tstrqService = new TStreamService("tstream_test_service", "TstrQ", "tstream test service",
      cassProv, cassandraTestKeyspace, cassProv, cassandraTestKeyspace, zkProv, "unit")
    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoService[Service]) = {
    serviceManager.delete("cassandra_test_service")
    serviceManager.delete("zookeeper_test_service")
    serviceManager.delete("tstream_test_service")
  }

  def createStreams(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], outputCount: Int) = {
    (1 to outputCount).foreach(x => {
      createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
      instanceOutputs = instanceOutputs :+ s"test-output-tstream$x"
    })
  }

  def deleteStreams(streamService: GenericMongoService[SjStream], outputCount: Int) = {
    (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))

  }

  private def createOutputTStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val localGenerator = new Generator("local")

    val tService = serviceManager.get("tstream_test_service")

    val s2 = new TStreamSjStream("test-output-tstream" + suffix, "test-output-tstream", partitions, tService, StreamConstants.tStreamType, Array("output", "some tags"), localGenerator)
    sjStreamService.save(s2)

    BasicStreamService.createStream(
      "test-output-tstream" + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream",
      metadataStorage,
      dataStorageFactory.getInstance(dataStorageOptions)
    )
  }

  private def deleteOutputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete("test-output-tstream" + suffix)
    BasicStreamService.deleteStream("test-output-tstream" + suffix, metadataStorage)
  }

  def createInstance(serviceManager: GenericMongoService[Service],
                     instanceService: GenericMongoService[Instance],
                     checkpointInterval: Int
                      ) = {

    val instance = new InputInstance()
    instance.name = instanceName
    instance.moduleType = "input-streaming"
    instance.moduleName = "com.bwsw.input.streaming.engine"
    instance.moduleVersion = "0.1"
    instance.status = "ready"
    instance.description = "some description of test instance"
    instance.outputs = instanceOutputs
    instance.checkpointMode = "every-nth"
    instance.checkpointInterval = checkpointInterval
    instance.parallelism = 1
    instance.options = """{"hey": "hey"}"""
    instance.perTaskCores = 0.1
    instance.perTaskRam = 64
    instance.performanceReportingInterval = 10000
    instance.engine = "com.bwsw.input.streaming.engine-0.1"
    instance.coordinationService = serviceManager.get("zookeeper_test_service").asInstanceOf[ZKService]
    instance.duplicateCheck = false
    instance.lookupHistory = 100
    instance.queueMaxSize = 100
    instance.defaultEvictionPolicy = "LRU"
    instance.evictionPolicy = "expanded-time"
    instance.tasks = tasks

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

  def createOutputConsumer(streamService: GenericMongoService[SjStream], suffix: String) = {
    createConsumer("test-output-tstream" + suffix, streamService, "localhost:805" + suffix)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoService[SjStream], address: String): Consumer[Array[Byte]] = {
    val stream = streamService.get(streamName)

    val tStream: TStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, metadataStorage, dataStorageFactory.getInstance(dataStorageOptions))

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
}
