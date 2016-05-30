package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.ArrayBlockingQueue

import com.aerospike.client.Host
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.DAL.model.{ZKService, SjStream, TStreamService, FileMetadata}
import com.bwsw.sj.common.DAL.model.module.{OutputInstance, Instance}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.engine.output.subscriber.OutputSubscriberCallback
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.consumer.{BasicConsumerOptions, ConsumerCoordinationSettings}
import com.bwsw.tstreams.agents.consumer.Offsets.IOffset
import com.bwsw.tstreams.converter.IConverter
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike.{AerospikeStorage, AerospikeStorageOptions, AerospikeStorageFactory}
import com.bwsw.tstreams.generator.LocalTimeUUIDGenerator
import com.bwsw.tstreams.metadata.MetadataStorageFactory

import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.DAL.ConnectionConstants._
import com.bwsw.tstreams.policy.RoundRobinPolicy
import com.bwsw.tstreams.services.BasicStreamService

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
  * Created: 27/05/2016
  *
  * @author Kseniya Tomskikh
  */
class OutputTaskManager {

  val instanceName: String = System.getenv("INSTANCE_NAME")
  val taskName: String = System.getenv("TASK_NAME")

  val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  val fileMetadataDAO: GenericMongoService[FileMetadata] = ConnectionRepository.getFileMetadataService
  val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  val streamDAO = ConnectionRepository.getStreamService

  private val converter = new IConverter[Array[Byte], Array[Byte]] {
    override def convert(obj: Array[Byte]): Array[Byte] = {
      obj
    }
  }

  def getFileMetadata(instance: OutputInstance) = {
    fileMetadataDAO.getByParameters(Map("specification.name" -> instance.moduleName,
      "specification.module-type" -> instance.moduleType,
      "specification.version" -> instance.moduleVersion)).head
  }

  def getModuleJar(instance: OutputInstance) = {
    val fileMetadata = getFileMetadata(instance)
    fileStorage.get(fileMetadata.filename, s"tmp/${instance.moduleName}")
  }

  def getOutputInstance = {
    instanceDAO.get(instanceName).asInstanceOf[OutputInstance]
  }

  def getMetadataStorage(service: TStreamService) = {
    val storageFactory = new MetadataStorageFactory
    val hosts = service.metadataProvider.hosts.map {addr =>
      val parts = addr.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toList
    storageFactory.getInstance(hosts, service.metadataNamespace)
  }

  def getDataStorage(service: TStreamService): AerospikeStorage = {
    val storageFactory = new AerospikeStorageFactory
    val hosts = service.dataProvider.hosts.map {addr =>
      val parts = addr.split(":")
      new Host(parts(0), parts(1).toInt)
    }.toList
    val options = new AerospikeStorageOptions(service.dataNamespace, hosts)
    storageFactory.getInstance(options)
  }

  def getInputStream(instance: OutputInstance) = {
    streamDAO.get(instance.inputs.head)
  }

  def getOutputStream(instance: Instance) = {
    streamDAO.get(instance.outputs.head)
  }


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
    val service = stream.service.asInstanceOf[TStreamService]
    val dataStorage: IStorage[Array[Byte]] = getDataStorage(service)
    val metadataStorage = getMetadataStorage(service)

    val zkHosts = service.lockProvider.hosts.map { s =>
      val parts = s.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toList

    val coordinatorSettings = new ConsumerCoordinationSettings(
      "localhost:8889",//todo где брать
      service.lockNamespace,
      zkHosts,
      7000 //todo
    )

    val basicStream = BasicStreamService.loadStream(stream.name, metadataStorage, dataStorage)

    val roundRobinPolicy = new RoundRobinPolicy(basicStream, (partitions.head to partitions.tail.head).toList)

    val timeUuidGenerator =
      stream.generator.generatorType match {
        case "local" => new LocalTimeUUIDGenerator
        case _type =>
          val service = stream.generator.service.asInstanceOf[ZKService]
          val zkHosts = service.provider.hosts
          val prefix = service.namespace + "/" + {
            if (_type == "global") _type else stream.name
          }

          new NetworkTimeUUIDGenerator(zkHosts, prefix, retryInterval, retryCount)
      }

    val callback = new OutputSubscriberCallback(queue)

    val options = new BasicConsumerOptions[Array[Byte], Array[Byte]](
      transactionsPreload = 10,
      dataPreload = 7,
      consumerKeepAliveInterval = 5,
      converter,
      roundRobinPolicy,
      coordinatorSettings,
      offset,
      timeUuidGenerator,
      useLastOffset = true)

    new BasicSubscribingConsumer[Array[Byte], Array[Byte]](
      s"consumer-$taskName-${stream.name}",
      basicStream,
      options,
      callback,
      persistentQueuePath
    )
  }

  def getModuleHandler(file: File, handlerClassName: String) = {
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(handlerClassName)
    clazz.newInstance().asInstanceOf[OutputStreamingHandler]
  }



}
