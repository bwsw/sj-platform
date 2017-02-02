package com.bwsw.sj.engine.input

import java.io.{BufferedReader, File, InputStreamReader, PrintStream}
import java.net.Socket
import java.util
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask, Instance}
import com.bwsw.sj.common.DAL.model.{Service, _}
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.utils.{ProviderLiterals, _}
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}
import com.bwsw.tstreams.generator.LocalTransactionGenerator
import com.bwsw.tstreams.streams.StreamService

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
  private val zookeeperProvider = new Provider("zookeeper-test-provider", "zookeeper provider",
    zookeeperHosts, "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamService("tstream-test-service", ServiceLiterals.tstreamsType,
    "tstream test service", zookeeperProvider, "/input_prefix", "token")
  private val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()

  val outputCount = 2

  private def setTStreamFactoryProperties() = {
    setMetadataClusterProperties(tstrqService)
    setDataClusterProperties(tstrqService)
    setCoordinationOptions(tstrqService)
  }

  private def setMetadataClusterProperties(tStreamService: TStreamService) = {
//    tstreamFactory.setProperty(TSF_Dictionary.Metadata.Cluster.NAMESPACE, tStreamService.metadataNamespace)
//      .setProperty(TSF_Dictionary.Metadata.Cluster.ENDPOINTS, tStreamService.metadataProvider.hosts.mkString(","))
  } //todo after integration with t-streams

  private def setDataClusterProperties(tStreamService: TStreamService) = {
//    tStreamService.dataProvider.providerType match {
//      case ProviderLiterals.aerospikeType =>
//        tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.DRIVER, TSF_Dictionary.Data.Cluster.Consts.DATA_DRIVER_AEROSPIKE)
//      case _ =>
//        tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.DRIVER, TSF_Dictionary.Data.Cluster.Consts.DATA_DRIVER_CASSANDRA)
//    }
//
//    tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.NAMESPACE, tStreamService.dataNamespace)
//      .setProperty(TSF_Dictionary.Data.Cluster.ENDPOINTS, tStreamService.dataProvider.hosts.mkString(","))
  } //todo after integration with t-streams

  private def setCoordinationOptions(tStreamService: TStreamService) = {
//    tstreamFactory.setProperty(TSF_Dictionary.Coordination.ROOT, s"/${tStreamService.lockNamespace}")
//      .setProperty(TSF_Dictionary.Coordination.ENDPOINTS, tStreamService.lockProvider.hosts.mkString(","))
  } //todo after integration with t-streams

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


  def createProviders(providerService: GenericMongoService[Provider]) = {
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete("zookeeper-test-provider")
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val zkService = new ZKService("zookeeper-test-service", ServiceLiterals.zookeeperType, "zookeeper test service", zookeeperProvider, testNamespace)
    serviceManager.save(zkService)

    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoService[Service]) = {
    serviceManager.delete("zookeeper-test-service")
    serviceManager.delete("tstream-test-service")
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
    val s2 = new TStreamSjStream("test-output-tstream" + suffix, "test-output-tstream", partitions, tstrqService, StreamLiterals.tstreamType, Array("output", "some tags"))
    sjStreamService.save(s2)

    StreamService.createStream(
      "test-output-tstream" + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream",
      null,
      null
    )//todo after integration with t-streams
  }

  private def deleteOutputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete("test-output-tstream" + suffix)

    StreamService.deleteStream("test-output-tstream" + suffix, null) //todo after integration with t-streams
  }

  def createInstance(serviceManager: GenericMongoService[Service],
                     instanceService: GenericMongoService[Instance],
                     checkpointInterval: Int
                      ) = {

    val instance = new InputInstance()
    instance.name = instanceName
    instance.moduleType = EngineLiterals.inputStreamingType
    instance.moduleName = "input-streaming-stub"
    instance.moduleVersion = "1.0"
    instance.status = EngineLiterals.ready
    instance.description = "some description of test instance"
    instance.outputs = instanceOutputs
    instance.checkpointMode = EngineLiterals.everyNthMode
    instance.checkpointInterval = checkpointInterval
    instance.parallelism = 1
    instance.options = """{"hey": "hey"}"""
    instance.perTaskCores = 0.1
    instance.perTaskRam = 64
    instance.performanceReportingInterval = 10000
    instance.engine = "com.bwsw.input.streaming.engine-1.0"
    instance.coordinationService = serviceManager.get("zookeeper-test-service").get.asInstanceOf[ZKService]
    instance.duplicateCheck = false
    instance.lookupHistory = 100
    instance.queueMaxSize = 100
    instance.defaultEvictionPolicy = EngineLiterals.lruDefaultEvictionPolicy
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
    createConsumer("test-output-tstream" + suffix, streamService)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoService[SjStream]): Consumer[Array[Byte]] = {
    val stream = streamService.get(streamName).get.asInstanceOf[TStreamSjStream]
    val transactionGenerator = new LocalTransactionGenerator

    setStreamOptions(stream)

    tstreamFactory.getConsumer[Array[Byte]](
      streamName,
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
}
