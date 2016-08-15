package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.ConfigConstants._
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{Instance, OutputInstance}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.StreamConstants._
import com.bwsw.tstreams.common.CassandraConnectorConf
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike
import com.bwsw.tstreams.data.cassandra
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.services.BasicStreamService
import com.bwsw.tstreams.streams.TStream
import org.slf4j.LoggerFactory

/**
 * Data factory of output streaming engine
 * Created: 26/05/2016
 *
 * @author Kseniya Tomskikh
 */
object OutputDataFactory {

  private val logger = LoggerFactory.getLogger(getClass.getName)

  val instanceName: String = System.getenv("INSTANCE_NAME")
  val taskName: String = System.getenv("TASK_NAME")
  val agentHost: String = System.getenv("AGENTS_HOST")
  val agentsPorts: Array[String] = System.getenv("AGENTS_PORTS").split(",")

  assert(agentsPorts.length == 2, "Not enough ports for t-stream consumers/producers ")

  private val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  private val fileMetadataDAO: GenericMongoService[FileMetadata] = ConnectionRepository.getFileMetadataService
  private val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  private val streamDAO = ConnectionRepository.getStreamService
  private val reportStreamName = instanceName + "_report"

  val instance: OutputInstance = instanceDAO.get(instanceName).asInstanceOf[OutputInstance]

  val inputStream: SjStream = streamDAO.get(instance.inputs.head)
  private val tstreamService = inputStream.service.asInstanceOf[TStreamService]
  val outputStream: SjStream = streamDAO.get(instance.outputs.head)

  val inputStreamService = inputStream.service.asInstanceOf[TStreamService]

  private val metadataStorageFactory: MetadataStorageFactory = new MetadataStorageFactory
  private val cassandraConnectorConf = CassandraConnectorConf.apply(inputStreamService.metadataProvider.hosts.map { addr =>
    val parts = addr.split(":")
    new InetSocketAddress(parts(0), parts(1).toInt)
  }.toSet)
  val metadataStorage: MetadataStorage = metadataStorageFactory.getInstance(cassandraConnectorConf, inputStreamService.metadataNamespace)

  private val configService = ConnectionRepository.getConfigService
  private val streamTTL = configService.get(streamTTLTag).value.toInt

  val txnPreload = configService.get(txnPreloadTag).value.toInt
  val dataPreload = configService.get(dataPreloadTag).value.toInt
  val zkSessionTimeout = configService.get(zkSessionTimeoutTag).value.toInt
  val zkConnectionTimeout = configService.get(zkConnectionTimeoutTag).value.toInt
  val zkHosts = tstreamService.lockProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList
  val transportTimeout = configService.get(transportTimeoutTag).value.toInt
  val retryPeriod = configService.get(tgClientRetryPeriodTag).value.toInt
  val retryCount = configService.get(tgRetryCountTag).value.toInt
  val txnTTL = configService.get(txnTTLTag).value.toInt
  val txnKeepAliveInterval = configService.get(txnKeepAliveIntervalTag).value.toInt

  /**
   * Get metadata of module file
   *
   * @return FileMetadata entity
   */
  def getFileMetadata: FileMetadata = {
    logger.debug(s"Task $taskName. Get metadata of file.")
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
    logger.debug(s"Task $taskName. Get jar-file of module.")
    val fileMetadata = getFileMetadata
    fileStorage.get(fileMetadata.filename, s"tmp/${instance.moduleName}")
  }

  /**
   * Create data storage for producer/consumer settings
   */
  def createDataStorage() = {
    logger.debug(s"Task $taskName. Get data storage for service ${tstreamService.name}.")
    OutputDataFactory.tstreamService.dataProvider.providerType match {
      case "aerospike" =>
        val options = new aerospike.Options(
          OutputDataFactory.tstreamService.dataNamespace,
          OutputDataFactory.tstreamService.dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toList)

        (new aerospike.Factory).getInstance(options)

      case _ =>
        (new cassandra.Factory).getInstance(cassandraConnectorConf, OutputDataFactory.tstreamService.dataNamespace)
    }
  }

  /**
   * Creates t-stream or loads an existing t-stream to keep the reports of module performance.
   * For each task there is specific partition (task number = partition number).
   *
   * @return SjStream used for keeping the reports of module performance
   */
  def getReportStream = {

    getTStream(
      reportStreamName,
      "store reports of performance metrics",
      Array("report", "performance"),
      instance.parallelism
    )
  }

  private def getTStream(name: String, description: String, tags: Array[String], partitions: Int) = {
    logger.debug(s"Task $taskName. Get t-stream $name.")
    var tStream: TStream[Array[Byte]] = null
    val dataStorage: IStorage[Array[Byte]] = createDataStorage()

    if (BasicStreamService.isExist(name, metadataStorage)) {
      logger.debug(s"Task $taskName. Load t-stream.")
      tStream = BasicStreamService.loadStream(name, metadataStorage, dataStorage)
    } else {
      logger.debug(s"Task $taskName. Create t-stream.")
      tStream = BasicStreamService.createStream(
        name,
        partitions,
        streamTTL,
        description,
        metadataStorage,
        dataStorage
      )
    }

    new TStreamSjStream(
      tStream.getName,
      tStream.getDescriptions,
      tStream.getPartitions,
      tstreamService,
      tStreamType,
      tags,
      new Generator("local")
    )
  }
}
