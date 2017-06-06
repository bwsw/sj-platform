package com.bwsw.sj.engine.core.managment

import java.io.File
import java.net.URLClassLoader

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.instance.ExecutionPlan
import com.bwsw.sj.common.dal.model.module._
import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.engine.{EnvelopeDataSerializer, ExtendedEnvelopeDataSerializer, StreamingExecutor}
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.si.model.instance.{Instance, InstanceConversion}
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.StreamLiterals._
import com.bwsw.sj.engine.core.config.EngineConfigNames
import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.IOffset
import com.bwsw.tstreams.agents.consumer.subscriber.{Callback, Subscriber}
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.Producer
import com.bwsw.tstreams.storage.StorageClient
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Class allows to manage an environment of streaming task of specific type
  * The main methods allow:
  * 1) create stream in storage (kind of storage depends on t-stream implementation)
  * 2) get an executor created via reflection
  * 3) create t-stream consumers/subscribers and producers
  * for this purposes firstly [[TStreamsFactory]] is configured using [[com.bwsw.sj.common.dal.model.instance.InstanceDomain]]
  */
abstract class TaskManager(implicit injector: Injector) {
  protected val connectionRepository = inject[ConnectionRepository]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val streamRepository: GenericMongoRepository[StreamDomain] = connectionRepository.getStreamRepository

  private val config = ConfigFactory.load()

  val instanceName: String = config.getString(EngineConfigNames.instanceName)
  val agentsHost: String = config.getString(EngineConfigNames.agentsHost)
  private val agentsPorts = config.getString(EngineConfigNames.agentsPorts).split(",").map(_.toInt)
  protected val numberOfAgentsPorts: Int = agentsPorts.length
  val taskName: String = config.getString(EngineConfigNames.taskName)
  val instance: Instance = getInstance()
  protected val auxiliarySJTStream: StreamDomain = getAuxiliaryTStream()
  protected val auxiliaryTStreamService: TStreamServiceDomain = getAuxiliaryTStreamService()
  protected val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()

  protected var currentPortNumber: Int = 0
  private val storage: MongoFileStorage = connectionRepository.getFileStorage

  protected val fileMetadata: FileMetadataDomain = connectionRepository.getFileMetadataRepository.getByParameters(
    Map("specification.name" -> instance.moduleName,
      "specification.module-type" -> instance.moduleType,
      "specification.version" -> instance.moduleVersion)
  ).head
  private val executorClassName: String = fileMetadata.specification.executorClass
  val moduleClassLoader: URLClassLoader = createClassLoader()

  protected val executorClass: Class[_] = moduleClassLoader.loadClass(executorClassName)

  val envelopeDataSerializer: EnvelopeDataSerializer[AnyRef] =
    new ExtendedEnvelopeDataSerializer(moduleClassLoader, instance)
  val inputs: mutable.Map[StreamDomain, Array[Int]]


  private def getInstance(): Instance = {
    val maybeInstance = connectionRepository.getInstanceRepository.get(instanceName).map(inject[InstanceConversion].from)
    if (maybeInstance.isDefined) {

      if (maybeInstance.get.status != started) {
        throw new InterruptedException(s"Task cannot be started because of '${maybeInstance.get.status}' status of instance")
      }
    }
    else throw new NoSuchElementException(s"Instance is named '$instanceName' has not found")

    maybeInstance.orNull
  }

  private def getAuxiliaryTStream(): StreamDomain =
    instance.streams.flatMap(streamRepository.get).filter(_.streamType == tstreamType).head

  private def getAuxiliaryTStreamService(): TStreamServiceDomain = {
    auxiliarySJTStream.service.asInstanceOf[TStreamServiceDomain]
  }

  private def setTStreamFactoryProperties(): Unit = {
    setAuthOptions(auxiliaryTStreamService)
    setCoordinationOptions(auxiliaryTStreamService)
    setBindHostForAgents()
    applyConfigurationSettings()
  }

  private def setAuthOptions(tStreamService: TStreamServiceDomain): TStreamsFactory = {
    tstreamFactory.setProperty(ConfigurationOptions.Common.authenticationKey, tStreamService.token)
  }

  private def setCoordinationOptions(tStreamService: TStreamServiceDomain): TStreamsFactory = {
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.endpoints, tStreamService.provider.getConcatenatedHosts())
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.path, tStreamService.prefix)
  }

  private def setBindHostForAgents(): TStreamsFactory = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindPort, agentsHost)
    tstreamFactory.setProperty(ConfigurationOptions.Consumer.Subscriber.bindHost, agentsHost)
  }

  private def applyConfigurationSettings(): Unit = {
    val configService = connectionRepository.getConfigRepository

    val tstreamsSettings = configService.getByParameters(Map("domain" -> ConfigLiterals.tstreamsDomain))
    tstreamsSettings.foreach(x => tstreamFactory.setProperty(ConfigurationSetting.clearConfigurationSettingName(x.domain, x.name), x.value))
  }

  protected def createClassLoader(): URLClassLoader = {
    val file = getModuleJar
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get class loader for jar file: ${file.getName}.")

    val classLoaderUrls = Array(file.toURI.toURL)

    new URLClassLoader(classLoaderUrls, ClassLoader.getSystemClassLoader)
  }

  private def getModuleJar: File = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get file contains uploaded '${instance.moduleName}' module jar.")
    storage.get(fileMetadata.filename, s"tmp/${instance.moduleName}")
  }

  @throws(classOf[Exception])
  protected def getInputs(executionPlan: ExecutionPlan): mutable.Map[StreamDomain, Array[Int]] = {
    val task = executionPlan.tasks.get(taskName)

    Option(task) match {
      case None => throw new NullPointerException("There is no task with that name in the execution plan.")
      case _ => task.inputs.asScala.map(x => (streamRepository.get(x._1).get, x._2))

    }
  }

  /**
    * Create t-stream producers for each output stream
    *
    * @return map where key is stream name and value is t-stream producer
    */
  protected def createOutputProducers(): Map[String, Producer] = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create the t-stream producers for each output stream.")

    tstreamFactory.setProperty(ConfigurationOptions.Producer.Transaction.batchSize, 20)

    instance.outputs
      .map(x => (x, streamRepository.get(x).get))
      .map(x => (x._1, createProducer(x._2.asInstanceOf[TStreamStreamDomain]))).toMap
  }

  def createProducer(stream: TStreamStreamDomain): Producer = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create producer for stream: ${stream.name}.")

    setProducerBindPort()
    setStreamOptions(stream)

    tstreamFactory.getProducer(
      "producer_for_" + taskName + "_" + stream.name,
      (0 until stream.partitions).toSet)
  }

  private def setProducerBindPort(): Unit = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindPort, agentsPorts(currentPortNumber))
    currentPortNumber += 1
  }

  def createStorageStream(name: String, description: String, partitions: Int): Unit = {
    val streamTTL = tstreamFactory.getProperty(ConfigurationOptions.Stream.ttlSec).asInstanceOf[Int]
    val storageClient: StorageClient = tstreamFactory.getStorageClient()

    if (!storageClient.checkStreamExists(name)) {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
        s"Create t-stream: $name to $description.")
      storageClient.createStream(
        name,
        partitions,
        streamTTL,
        description
      )
    }

    storageClient.shutdown()
  }

  def getStream(name: String, description: String, tags: Array[String], partitions: Int): TStreamStreamDomain = {
    new TStreamStreamDomain(name, auxiliaryTStreamService, partitions)
  }

  /**
    * Creates a t-stream consumer with pub/sub property
    *
    * @param stream     stream [[TStreamStreamDomain]] from which massages are consumed
    * @param partitions range of stream partition
    * @param offset     offset policy [[IOffset]] that describes where a consumer starts
    * @param callback   subscriber callback for t-stream consumer
    * @return T-stream subscribing consumer
    */
  def createSubscribingConsumer(stream: TStreamStreamDomain,
                                partitions: List[Int],
                                offset: IOffset,
                                callback: Callback): Subscriber = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create subscribing consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head}).")

    val partitionRange = (partitions.head to partitions.tail.head).toSet

    setStreamOptions(stream)
    setSubscribingConsumerBindPort()

    tstreamFactory.getSubscriber(
      "subscribing_consumer_for_" + taskName + "_" + stream.name,
      partitionRange,
      callback,
      offset)
  }

  /**
    * Creates a t-stream consumer
    *
    * @param stream     stream [[TStreamStreamDomain]] from which massages are consumed
    * @param partitions range of stream partition
    * @param offset     offset policy [[IOffset]] that describes where a consumer starts
    * @param name       name of consumer (it is optional parameter)
    * @return T-stream consumer
    */
  def createConsumer(stream: TStreamStreamDomain,
                     partitions: List[Int],
                     offset: IOffset,
                     name: Option[String] = None): Consumer = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head}).")
    val consumerName = name match {
      case Some(_name) => _name
      case None => "consumer_for_" + taskName + "_" + stream.name
    }

    setStreamOptions(stream)

    tstreamFactory.getConsumer(
      consumerName,
      (0 until stream.partitions).toSet,
      offset)
  }

  def createCheckpointGroup(): CheckpointGroup = {
    tstreamFactory.getCheckpointGroup()
  }

  protected def setStreamOptions(stream: TStreamStreamDomain): TStreamsFactory = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, stream.name)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, stream.partitions)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.description, stream.description)
  }

  private def setSubscribingConsumerBindPort(): Unit = {
    tstreamFactory.setProperty(ConfigurationOptions.Consumer.Subscriber.bindPort, agentsPorts(currentPortNumber))
    currentPortNumber += 1
  }

  /**
    * @return executor of module that has got an environment manager
    */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor
}
