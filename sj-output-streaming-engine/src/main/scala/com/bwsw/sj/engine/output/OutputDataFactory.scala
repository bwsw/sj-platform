package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.DAL.model.module.{OutputInstance, Instance}
import com.bwsw.sj.common.DAL.model.{FileMetadata, TStreamService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.tstreams.data.aerospike.{AerospikeStorage, AerospikeStorageOptions, AerospikeStorageFactory}
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}

/**
  * Data factory of output streaming engine
  * Created: 5/26/16
  *
  * @author Kseniya Tomskikh
  */
object OutputDataFactory {

  val instanceName: String = System.getenv("INSTANCE_NAME")
  val taskName: String = System.getenv("TASK_NAME")

  private val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  private val fileMetadataDAO: GenericMongoService[FileMetadata] = ConnectionRepository.getFileMetadataService
  private val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  private val streamDAO = ConnectionRepository.getStreamService

  val instance: OutputInstance = instanceDAO.get(instanceName).asInstanceOf[OutputInstance]

  val inputStream: SjStream = streamDAO.get(instance.inputs.head)
  val outputStream: SjStream = streamDAO.get(instance.outputs.head)

  val inputStreamService = inputStream.service.asInstanceOf[TStreamService]

  private val metadataStorageFactory = new MetadataStorageFactory
  private val metadataStorageHosts = inputStreamService.metadataProvider.hosts.map { addr =>
    val parts = addr.split(":")
    new InetSocketAddress(parts(0), parts(1).toInt)
  }.toList
  val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(metadataStorageHosts, inputStreamService.metadataNamespace)

  private val dataStorageFactory = new AerospikeStorageFactory
  private val dataStorageHosts = inputStreamService.dataProvider.hosts.map {addr =>
      val parts = addr.split(":")
      new Host(parts(0), parts(1).toInt)
    }.toList
  private  val options = new AerospikeStorageOptions(inputStreamService.dataNamespace, dataStorageHosts)
  val dataStorage: AerospikeStorage = dataStorageFactory.getInstance(options)

  def getFileMetadata: FileMetadata = {
    fileMetadataDAO.getByParameters(Map("specification.name" -> instance.moduleName,
      "specification.module-type" -> instance.moduleType,
      "specification.version" -> instance.moduleVersion)).head
  }

  def getModuleJar: File = {
    val fileMetadata = getFileMetadata
    fileStorage.get(fileMetadata.filename, s"tmp/${instance.moduleName}")
  }

}
