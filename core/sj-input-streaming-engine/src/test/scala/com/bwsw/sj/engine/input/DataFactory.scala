package com.bwsw.sj.engine.input

import java.io.{BufferedReader, File, InputStreamReader}
import java.util
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask, Instance}
import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.model.service.{Service, TStreamService, ZKService}
import com.bwsw.sj.common.DAL.model.stream.{SjStream, TStreamSjStream}
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.utils.{ProviderLiterals, _}
import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}

object DataFactory {
  private val zookeeperHosts = System.getenv("ZOOKEEPER_HOSTS")
  private val testNamespace = "test_namespace_for_input_engine"
  private val instanceName = "test-instance-for-input-engine"
  private val zookeeperProviderName = "zookeeper-test-provider"
  private val tstreamServiceName = "tstream-test-service"
  private val zookeeperServiceName = "zookeeper-test-service"
  private val tstreamOutputNamePrefix = "tstream-output"
  private var instanceOutputs: Array[String] = Array()
  private val tasks = new util.HashMap[String, InputTask]()
  tasks.put(s"$instanceName-task0", new InputTask(SjInputServices.host, SjInputServices.port))
  private val partitions = 1
  private val serializer = new JsonSerializer()
  private val zookeeperProvider = new Provider(zookeeperProviderName, zookeeperProviderName, zookeeperHosts.split(","), "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamService(tstreamServiceName, ServiceLiterals.tstreamsType,
    tstreamServiceName, zookeeperProvider, TestStorageServer.prefix, TestStorageServer.token)
  private val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()
  val storageClient = tstreamFactory.getStorageClient()

  val outputCount = 2

  private def setTStreamFactoryProperties() = {
    setAuthOptions(tstrqService)
    setStorageOptions(tstrqService)
    setCoordinationOptions(tstrqService)
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

  def createProviders(providerService: GenericMongoService[Provider]) = {
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoService[Provider]) = {
    providerService.delete(zookeeperProviderName)
  }

  def createServices(serviceManager: GenericMongoService[Service], providerService: GenericMongoService[Provider]) = {
    val zkService = new ZKService(zookeeperServiceName, ServiceLiterals.zookeeperType, zookeeperServiceName, zookeeperProvider, testNamespace)
    serviceManager.save(zkService)

    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoService[Service]) = {
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(tstreamServiceName)
  }

  def createStreams(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], outputCount: Int) = {
    (1 to outputCount).foreach(x => {
      createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
      instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
    })
  }

  def deleteStreams(streamService: GenericMongoService[SjStream], outputCount: Int) = {
    (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
  }

  private def createOutputTStream(sjStreamService: GenericMongoService[SjStream], serviceManager: GenericMongoService[Service], partitions: Int, suffix: String) = {
    val s2 = new TStreamSjStream(
      tstreamOutputNamePrefix + suffix,
      tstreamOutputNamePrefix,
      partitions,
      tstrqService,
      StreamLiterals.tstreamType,
      Array("output", "some tags"))

    sjStreamService.save(s2)

    storageClient.createStream(
      tstreamOutputNamePrefix + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream")
  }

  private def deleteOutputTStream(streamService: GenericMongoService[SjStream], suffix: String) = {
    streamService.delete(tstreamOutputNamePrefix + suffix)

    storageClient.deleteStream(tstreamOutputNamePrefix + suffix)
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
    instance.status = EngineLiterals.started
    instance.description = "some description of test instance"
    instance.outputs = instanceOutputs
    instance.checkpointMode = EngineLiterals.everyNthMode
    instance.checkpointInterval = checkpointInterval
    instance.engine = "com.bwsw.input.streaming.engine-1.0"
    instance.coordinationService = serviceManager.get(zookeeperServiceName).get.asInstanceOf[ZKService]
    instance.duplicateCheck = false
    instance.lookupHistory = 100
    instance.queueMaxSize = 500
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
    createConsumer(tstreamOutputNamePrefix + suffix, streamService)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoService[SjStream]): Consumer = {
    val stream = streamService.get(streamName).get.asInstanceOf[TStreamSjStream]

    setStreamOptions(stream)

    tstreamFactory.getConsumer(
      streamName,
      (0 until stream.partitions).toSet,
      Oldest)
  }

  protected def setStreamOptions(stream: TStreamSjStream) = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, stream.name)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, stream.partitions)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.description, stream.description)
  }
}
