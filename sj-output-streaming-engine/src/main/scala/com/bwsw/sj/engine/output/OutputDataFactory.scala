package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.ConfigConstants._
import com.bwsw.sj.common.DAL.model.module.{Instance, OutputInstance}
import com.bwsw.sj.common.DAL.model.{FileMetadata, SjStream, TStreamService}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.tstreams.data.aerospike.{AerospikeStorage, AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}

/**
  * Data factory of output streaming engine
  * Created: 26/05/2016
  *
  * @author Kseniya Tomskikh
  */
object OutputDataFactory {

  val instanceName: String = System.getenv("INSTANCE_NAME")
  val taskName: String = System.getenv("TASK_NAME")
  private val agentHost: String = System.getenv("AGENTS_HOST")
  private val agentsPorts: Array[String] = System.getenv("AGENTS_PORTS").split(",")
  assert(agentsPorts.length == 1, "Not enough ports for t-stream consumers/producers ")
  private val agentPort: Int = agentsPorts.head.toInt
  val agentAddress = OutputDataFactory.agentHost + ":" + OutputDataFactory.agentPort.toString

  private val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  private val fileMetadataDAO: GenericMongoService[FileMetadata] = ConnectionRepository.getFileMetadataService
  private val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  private val streamDAO = ConnectionRepository.getStreamService

  val instance: OutputInstance = instanceDAO.get(instanceName).asInstanceOf[OutputInstance]

  val inputStream: SjStream = streamDAO.get(instance.inputs.head)
  val outputStream: SjStream = streamDAO.get(instance.outputs.head)

  val inputStreamService = inputStream.service.asInstanceOf[TStreamService]

  private val metadataStorageFactory: MetadataStorageFactory = new MetadataStorageFactory
  private val metadataStorageHosts: List[InetSocketAddress] = inputStreamService.metadataProvider.hosts.map { addr =>
    val parts = addr.split(":")
    new InetSocketAddress(parts(0), parts(1).toInt)
  }.toList
  val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(metadataStorageHosts, inputStreamService.metadataNamespace)

  private val dataStorageFactory: AerospikeStorageFactory = new AerospikeStorageFactory
  private val dataStorageHosts: List[Host] = inputStreamService.dataProvider.hosts.map { addr =>
      val parts = addr.split(":")
      new Host(parts(0), parts(1).toInt)
    }.toList
  private  val options = new AerospikeStorageOptions(inputStreamService.dataNamespace, dataStorageHosts)
  val dataStorage: AerospikeStorage = dataStorageFactory.getInstance(options)

  private val configService = ConnectionRepository.getConfigService

  val txnPreload = configService.get(txnPreloadTag).value.toInt
  val dataPreload = configService.get(dataPreloadTag).value.toInt
  val consumerKeepAliveInterval = configService.get(consumerKeepAliveInternalTag).value.toInt
  val zkTimeout = configService.get(zkSessionTimeoutTag).value.toInt
  /**
    * Get metadata of module file
    *
    * @return FileMetadata entity
    */
  def getFileMetadata: FileMetadata = {
    fileMetadataDAO.getByParameters(Map("specification.name" -> instance.moduleName,
      "specification.module-type" -> instance.moduleType,
      "specification.version" -> instance.moduleVersion)).head
  }

  /**
    * Get file for module
    *
    * @return Jar of module
    */
  def getModuleJar: File = {
    val fileMetadata = getFileMetadata
    fileStorage.get(fileMetadata.filename, s"tmp/${instance.moduleName}")
  }

}
