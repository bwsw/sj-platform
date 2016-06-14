package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.DAL.model.{TStreamSjStream, SjStream, TStreamService}
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.engine.core.converter.ArrayByteConverter
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.engine.core.utils.EngineUtils
import com.bwsw.sj.engine.output.subscriber.OutputSubscriberCallback
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.consumer.{SubscriberCoordinationOptions, BasicConsumerOptions}
import com.bwsw.tstreams.agents.consumer.Offsets.IOffset
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.generator.IUUIDGenerator

import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.collection.JavaConverters._

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
    val dataStorage: IStorage[Array[Byte]] = OutputDataFactory.dataStorage
    val metadataStorage = OutputDataFactory.metadataStorage

    val zkHosts = service.lockProvider.hosts.map { s =>
      val parts = s.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toList

    val agentHost = OutputDataFactory.agentHost
    val agentPort = OutputDataFactory.agentPort
    val agentAddress = s"${if (agentHost != null && !agentHost.equals("")) agentHost else "localhost"}" +
      s":${if (agentPort != null && !agentPort.equals("")) agentPort else "8889"}"

    val coordinatorSettings = new SubscriberCoordinationOptions(
      agentAddress,
      s"/${service.lockNamespace}",
      zkHosts,
      7000 //todo
    )

    val basicStream = BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (partitions.head to partitions.tail.head).toList)

    val timeUuidGenerator: IUUIDGenerator = EngineUtils.getUUIDGenerator(stream.asInstanceOf[TStreamSjStream])

    val callback = new OutputSubscriberCallback(queue)

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
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

}
