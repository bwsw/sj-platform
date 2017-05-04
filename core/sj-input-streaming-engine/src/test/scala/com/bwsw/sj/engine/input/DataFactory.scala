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
import com.bwsw.sj.common.DAL.service.GenericMongoRepository
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
  private val tstrqService = new TStreamService(tstreamServiceName, tstreamServiceName, zookeeperProvider, TestStorageServer.prefix, TestStorageServer.token)
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

  def createProviders(providerService: GenericMongoRepository[Provider]) = {
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoRepository[Provider]) = {
    providerService.delete(zookeeperProviderName)
  }

  def createServices(serviceManager: GenericMongoRepository[Service], providerService: GenericMongoRepository[Provider]) = {
    val zkService = new ZKService(zookeeperServiceName, zookeeperServiceName, zookeeperProvider, testNamespace)
    serviceManager.save(zkService)

    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoRepository[Service]) = {
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(tstreamServiceName)
  }

  def createStreams(sjStreamService: GenericMongoRepository[SjStream], serviceManager: GenericMongoRepository[Service], outputCount: Int) = {
    (1 to outputCount).foreach(x => {
      createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
      instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
    })
  }

  def deleteStreams(streamService: GenericMongoRepository[SjStream], outputCount: Int) = {
    (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
  }

  private def createOutputTStream(sjStreamService: GenericMongoRepository[SjStream], serviceManager: GenericMongoRepository[Service], partitions: Int, suffix: String) = {
    val s2 = new TStreamSjStream(
      tstreamOutputNamePrefix + suffix,
      tstrqService,
      partitions,
      tstreamOutputNamePrefix + suffix,
      false,
      Array("output", "some tags")
    )

    sjStreamService.save(s2)

    storageClient.createStream(
      tstreamOutputNamePrefix + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream")
  }

  private def deleteOutputTStream(streamService: GenericMongoRepository[SjStream], suffix: String) = {
    streamService.delete(tstreamOutputNamePrefix + suffix)

    storageClient.deleteStream(tstreamOutputNamePrefix + suffix)
  }

  def createInstance(serviceManager: GenericMongoRepository[Service],
                     instanceService: GenericMongoRepository[Instance],
                     checkpointInterval: Int
                    ) = {

    val instance = new InputInstance(instanceName, EngineLiterals.inputStreamingType,
      "input-streaming-stub", "1.0", "com.bwsw.input.streaming.engine-1.0",
      serviceManager.get(zookeeperServiceName).get.asInstanceOf[ZKService], EngineLiterals.everyNthMode
    )
    instance.status = EngineLiterals.started
    instance.description = "some description of test instance"
    instance.outputs = instanceOutputs
    instance.checkpointInterval = checkpointInterval
    instance.duplicateCheck = false
    instance.lookupHistory = 100
    instance.queueMaxSize = 500
    instance.defaultEvictionPolicy = EngineLiterals.lruDefaultEvictionPolicy
    instance.evictionPolicy = "expanded-time"
    instance.tasks = tasks

    instanceService.save(instance)
  }

  def deleteInstance(instanceService: GenericMongoRepository[Instance]) = {
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

  def createOutputConsumer(streamService: GenericMongoRepository[SjStream], suffix: String) = {
    createConsumer(tstreamOutputNamePrefix + suffix, streamService)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoRepository[SjStream]): Consumer = {
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
