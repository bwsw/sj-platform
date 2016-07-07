package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.engine.core.converter.ArrayByteConverter
import com.bwsw.sj.engine.core.entities.OutputEntity
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.engine.core.utils.EngineUtils
import com.bwsw.sj.engine.output.subscriber.OutputSubscriberCallback
import com.bwsw.tstreams.agents.consumer.Offsets.IOffset
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.consumer.{BasicConsumerOptions, SubscriberCoordinationOptions}
import com.bwsw.tstreams.agents.producer.InsertionType.SingleElementInsert
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerOptions, ProducerCoordinationOptions}
import com.bwsw.tstreams.coordination.transactions.transport.impl.TcpTransport
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.generator.{IUUIDGenerator, LocalTimeUUIDGenerator}
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.BasicStream
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
 * Task manager for working with streams of output-streaming module
 * Created: 27/05/2016
 *
 * @author Kseniya Tomskikh
 */
class OutputTaskManager(taskName: String, instance: OutputInstance) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  val task: mutable.Map[String, Array[Int]] = instance.executionPlan.tasks.get(taskName).inputs.asScala

  private val converter = new ArrayByteConverter

  /**
   * Creates a t-stream consumer with pub/sub property
   *
   * @param stream SjStream from which massages are consumed
   * @param partitions Range of stream partition
   * @param offset Offset policy that describes where a consumer starts
   * @param queue Queue which keeps consumed messages
   * @return T-stream subscribing consumer
   */
  def createSubscribingConsumer(stream: SjStream,
                                partitions: List[Int],
                                offset: IOffset,
                                queue: ArrayBlockingQueue[String]) = {
    logger.info(s"Creating subscribe consumer for stream: ${stream.name} of instance: ${instance.name}.")
    val service = stream.service.asInstanceOf[TStreamService]
    val dataStorage: IStorage[Array[Byte]] = OutputDataFactory.createDataStorage()
    val metadataStorage = OutputDataFactory.metadataStorage

    val zkHosts = service.lockProvider.hosts.map { s =>
      val parts = s.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toList

    val agentPort: Int = OutputDataFactory.agentsPorts.head.toInt

    val agentAddress = OutputDataFactory.agentHost + ":" + agentPort.toString

    val coordinatorSettings = new SubscriberCoordinationOptions(
      agentAddress,
      s"/${service.lockNamespace}",
      zkHosts,
      OutputDataFactory.zkSessionTimeout,
      OutputDataFactory.zkConnectionTimeout
    )

    val basicStream = BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (partitions.head to partitions.tail.head).toList)

    val timeUuidGenerator: IUUIDGenerator = EngineUtils.getUUIDGenerator(stream.asInstanceOf[TStreamSjStream])

    val callback = new OutputSubscriberCallback(queue)

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      OutputDataFactory.txnPreload,
      OutputDataFactory.dataPreload,
      OutputDataFactory.consumerKeepAliveInterval,
      converter,
      roundRobinPolicy,
      offset,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicSubscribingConsumer[Array[Byte], Array[Byte]](
      s"consumer-$taskName-${stream.name}",
      basicStream,
      options,
      coordinatorSettings,
      callback,
      persistentQueuePath
    )
  }

  /**
   * Creates a t-stream producer for recording messages
   *
   * @param stream SjStream to which messages are written
   * @return Basic t-stream producer
   */
  def createProducer(stream: SjStream) = {
    logger.debug(s"Instance name: ${instance.name}, task name: $taskName. " +
      s"Create basic producer for stream: ${stream.name}\n")
    val dataStorage: IStorage[Array[Byte]] = OutputDataFactory.createDataStorage()
    val service = stream.service.asInstanceOf[TStreamService]

    val coordinationOptions = new ProducerCoordinationOptions(
      agentAddress = OutputDataFactory.agentHost + ":" + OutputDataFactory.agentsPorts(1), //todo: number of agent port
      OutputDataFactory.zkHosts,
      "/" + service.lockNamespace,
      OutputDataFactory.zkSessionTimeout,
      isLowPriorityToBeMaster = false,
      transport = new TcpTransport,
      transportTimeout = OutputDataFactory.transportTimeout,
      zkConnectionTimeout = OutputDataFactory.zkConnectionTimeout
    )

    val basicStream: BasicStream[Array[Byte]] =
      BasicStreamService.loadStream(stream.name, OutputDataFactory.metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (0 until stream.asInstanceOf[TStreamSjStream].partitions).toList)

    val timeUuidGenerator =
      stream.asInstanceOf[TStreamSjStream].generator.generatorType match {
        case "local" => new LocalTimeUUIDGenerator
        case _type =>
          val service = stream.asInstanceOf[TStreamSjStream].generator.service.asInstanceOf[ZKService]
          val zkServers = service.provider.hosts
          val prefix = "/" + service.namespace + "/" + {
            if (_type == "global") _type else basicStream.name
          }

          new NetworkTimeUUIDGenerator(zkServers, prefix, OutputDataFactory.retryPeriod, OutputDataFactory.retryCount)
      }

    val options = new BasicProducerOptions[Array[Byte], Array[Byte]](
      OutputDataFactory.txnTTL,
      OutputDataFactory.txnKeepAliveInterval,
      OutputDataFactory.producerKeepAliveInterval,
      roundRobinPolicy,
      SingleElementInsert,
      timeUuidGenerator,
      coordinationOptions,
      converter)

    new BasicProducer[Array[Byte], Array[Byte]](
      "producer for " + taskName + "_" + stream.name,
      basicStream,
      options
    )
  }

  /**
   * Getting instance of handler object from module jar
   *
   * @param file Jar of module
   * @param handlerClassName Classname of handler class of module
   * @return Handler instance from jar
   */
  def getModuleHandler(file: File, handlerClassName: String) = {
    logger.info(s"Getting handler object from jar of file: ${instance.moduleType}-${instance.moduleName}-${instance.moduleVersion}")
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(handlerClassName)
    clazz.newInstance().asInstanceOf[OutputStreamingHandler]
  }

  /**
   * Getting instance of entity object from output module jar
   *
   * @param file Jar of module
   * @param entityClassName Classname of entity class of module
   * @return Entity instance from jar
   */
  def getOutputModuleEntity(file: File, entityClassName: String) = {
    logger.info(s"Getting entity object from jar of file: ${instance.moduleType}-${instance.moduleName}-${instance.moduleVersion}")
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(entityClassName)
    clazz.newInstance().asInstanceOf[OutputEntity]
  }
}
