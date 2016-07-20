package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.converter.ArrayByteConverter
import com.bwsw.sj.engine.core.entities.OutputEntity
import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.sj.engine.core.managment.TaskManager
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
class OutputTaskManager() extends TaskManager {

  val task: mutable.Map[String, Array[Int]] = instance.executionPlan.tasks.get(taskName).inputs.asScala


  /**
    * Returns an instance of executor of module
    *
    * @return An instance of executor of module
    */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor = ???

  /**
    * Returns instance of executor of module
    *
    * @return An instance of executor of module
    */
  def getExecutor: StreamingExecutor = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    val moduleJar = getModuleJar
    val classLoader = getClassLoader(moduleJar.getAbsolutePath)

    logger.debug(s"Task: $taskName. Create instance of executor class\n")
    val executor = classLoader
      .loadClass(fileMetadata.specification.executorClass)
      .newInstance()
      .asInstanceOf[OutputStreamingHandler]

    executor
  }

  /**
    * Getting instance of entity object from output module jar
    *
    * @param file Jar of module
    * @param entityClassName Classname of entity class of module
    * @return Entity instance from jar
    */
  def getOutputModuleEntity(file: File, entityClassName: String) = {
    logger.info(s"Task: $taskName. Getting entity object from jar of file: ${instance.moduleType}-${instance.moduleName}-${instance.moduleVersion}")
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(entityClassName)
    clazz.newInstance().asInstanceOf[OutputEntity]
  }
}
