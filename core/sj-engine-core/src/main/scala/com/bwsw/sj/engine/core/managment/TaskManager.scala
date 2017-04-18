package com.bwsw.sj.engine.core.managment

import java.io.File
import java.net.URLClassLoader

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.engine.{EnvelopeDataSerializer, ExtendedEnvelopeDataSerializer, StreamingExecutor}
import com.bwsw.sj.common.rest.entities.module.ExecutionPlan
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.StreamLiterals._
import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.tstreams.agents.consumer.Offset.IOffset
import com.bwsw.tstreams.agents.consumer.subscriber.Callback
import com.bwsw.tstreams.common.StorageClient
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class TaskManager() {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  val streamDAO = ConnectionRepository.getStreamService

  require(System.getenv("INSTANCE_NAME") != null &&
    System.getenv("TASK_NAME") != null &&
    System.getenv("AGENTS_HOST") != null &&
    System.getenv("AGENTS_PORTS") != null,
    "No environment variables: INSTANCE_NAME, TASK_NAME, AGENTS_HOST, AGENTS_PORTS")

  val instanceName = System.getenv("INSTANCE_NAME")
  val agentsHost = System.getenv("AGENTS_HOST")
  private val agentsPorts = System.getenv("AGENTS_PORTS").split(",").map(_.toInt)
  protected val numberOfAgentsPorts = agentsPorts.length
  val taskName = System.getenv("TASK_NAME")
  val instance: Instance = getInstance()
  protected val auxiliarySJTStream = getAuxiliaryTStream()
  protected val auxiliaryTStreamService = getAuxiliaryTStreamService()
  protected val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()

  protected var currentPortNumber = 0
  private val storage = ConnectionRepository.getFileStorage

  protected val fileMetadata: FileMetadata = ConnectionRepository.getFileMetadataService.getByParameters(
    Map("specification.name" -> instance.moduleName,
      "specification.module-type" -> instance.moduleType,
      "specification.version" -> instance.moduleVersion)
  ).head
  private val executorClassName = fileMetadata.specification.executorClass
  val moduleClassLoader = createClassLoader()

  protected val executorClass = moduleClassLoader.loadClass(executorClassName)

  val envelopeDataSerializer: EnvelopeDataSerializer[AnyRef] =
    new ExtendedEnvelopeDataSerializer(moduleClassLoader, instance)
  val inputs: mutable.Map[SjStream, Array[Int]]

  private def getInstance() = {
    val maybeInstance = ConnectionRepository.getInstanceService.get(instanceName)
    var instance: Instance = null
    if (maybeInstance.isDefined) {
      instance = maybeInstance.get

      if (instance.status != started) {
        throw new InterruptedException(s"Task cannot be started because of '${instance.status}' status of instance")
      }
    }
    else throw new NoSuchElementException(s"Instance is named '$instanceName' has not found")

    instance
  }

  private def getAuxiliaryTStream() = {
    val inputs = instance.getInputsWithoutStreamMode()
    val streams = inputs.union(instance.outputs)
    val sjStream = streams.flatMap(s => streamDAO.get(s)).filter(s => s.streamType.equals(tstreamType)).head

    sjStream
  }

  private def getAuxiliaryTStreamService() = {
    auxiliarySJTStream.service.asInstanceOf[TStreamService]
  }

  private def setTStreamFactoryProperties() = {
    setAuthOptions(auxiliaryTStreamService)
    setStorageOptions(auxiliaryTStreamService)
    setCoordinationOptions(auxiliaryTStreamService)
    setBindHostForAgents()
    setPersistentQueuePath()
    applyConfigurationSettings()
  }

  private def setAuthOptions(tStreamService: TStreamService) = {
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Auth.key, tStreamService.token)
  }

  private def setStorageOptions(tStreamService: TStreamService) = {
    logger.debug(s"Task name: $taskName. Set properties of storage " +
      s"(zookeeper endpoints: ${tStreamService.provider.hosts.mkString(",")}, prefix: ${tStreamService.prefix}) " +
      s"of t-stream factory\n")
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, tStreamService.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.prefix, tStreamService.prefix)
  }

  private def setCoordinationOptions(tStreamService: TStreamService) = {
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.endpoints, tStreamService.provider.hosts.mkString(","))
  }

  private def setBindHostForAgents() = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindPort, agentsHost)
    tstreamFactory.setProperty(ConfigurationOptions.Consumer.Subscriber.bindHost, agentsHost)
  }

  private def setPersistentQueuePath() = {
    tstreamFactory.setProperty(ConfigurationOptions.Consumer.Subscriber.persistentQueuePath, "/tmp/" + persistentQueuePath)
  }

  private def applyConfigurationSettings() = {
    val configService = ConnectionRepository.getConfigService

    val tstreamsSettings = configService.getByParameters(Map("domain" -> ConfigLiterals.tstreamsDomain))
    tstreamsSettings.foreach(x => tstreamFactory.setProperty(clearConfigurationSettingName(x.domain, x.name), x.value))
  }

  /**
    * Returns class loader for retrieving classes from jar
    *
    * @return Class loader for retrieving classes from jar
    */
  protected def createClassLoader() = {
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
  protected def getInputs(executionPlan: ExecutionPlan) = {
    val task = executionPlan.tasks.get(taskName)
    task match {
      case null => throw new NullPointerException("There is no task with that name in the execution plan.")
      case _ => task.inputs.asScala.map(x => (streamDAO.get(x._1).get, x._2))
    }
  }

  /**
    * Create t-stream producers for each output stream
    *
    * @return Map where key is stream name and value is t-stream producer
    */
  protected def createOutputProducers() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create the t-stream producers for each output stream.")

    tstreamFactory.setProperty(ConfigurationOptions.Producer.Transaction.batchSize, 20)

    instance.outputs
      .map(x => (x, streamDAO.get(x).get))
      .map(x => (x._1, createProducer(x._2.asInstanceOf[TStreamSjStream]))).toMap
  }

  def createProducer(stream: TStreamSjStream) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create producer for stream: ${stream.name}.")

    setProducerBindPort()
    setStreamOptions(stream)

    tstreamFactory.getProducer(
      "producer_for_" + taskName + "_" + stream.name,
      (0 until stream.partitions).toSet)
  }

  private def setProducerBindPort() = {
    tstreamFactory.setProperty(ConfigurationOptions.Producer.bindPort, agentsPorts(currentPortNumber))
    currentPortNumber += 1
  }

  def createTStreamOnCluster(name: String, description: String, partitions: Int): Unit = {
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
  }

  def getSjStream(name: String, description: String, tags: Array[String], partitions: Int) = {
    new TStreamSjStream(
      name,
      description,
      partitions,
      auxiliaryTStreamService,
      tstreamType,
      tags
    )
  }

  /**
    * Creates a t-stream consumer with pub/sub property
    *
    * @param stream     SjStream from which massages are consumed
    * @param partitions Range of stream partition
    * @param offset     Offset policy that describes where a consumer starts
    * @param callback   Subscriber callback for t-stream consumer
    * @return T-stream subscribing consumer
    */
  def createSubscribingConsumer(stream: TStreamSjStream,
                                partitions: List[Int],
                                offset: IOffset,
                                callback: Callback) = {
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

  protected def setStreamOptions(stream: TStreamSjStream) = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, stream.name)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, stream.partitions)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.description, stream.description)
  }

  private def setSubscribingConsumerBindPort() = {
    tstreamFactory.setProperty(ConfigurationOptions.Consumer.Subscriber.bindPort, agentsPorts(currentPortNumber))
    currentPortNumber += 1
  }

  /**
    * @return An instance of executor of module that has got an environment manager
    */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor
}
