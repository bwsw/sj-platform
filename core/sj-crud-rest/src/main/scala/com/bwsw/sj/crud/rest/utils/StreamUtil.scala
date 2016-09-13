package com.bwsw.sj.crud.rest.utils

import java.net.InetSocketAddress
import java.text.MessageFormat
import java.util.{Properties, ResourceBundle}

import com.aerospike.client.Host
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.rest.entities.stream.SjStreamData
import com.bwsw.sj.common.utils.{ProviderLiterals, GeneratorLiterals, ConfigSettingsUtils}
import com.bwsw.tstreams.common.CassandraConnectorConf
import com.bwsw.tstreams.data.aerospike._
import com.bwsw.tstreams.data.{IStorage, _}
import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}
import com.bwsw.tstreams.metadata.{MetadataStorage, MetadataStorageFactory}
import com.bwsw.tstreams.services.BasicStreamService
import kafka.admin.AdminUtils
import kafka.common.{TopicAlreadyMarkedForDeletionException, TopicExistsException}
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object StreamUtil {
  private val logger = LoggerFactory.getLogger(getClass.getName)

  def chackAndCreate(initialData: SjStreamData, stream: SjStream): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    stream match {
      case s: TStreamSjStream =>
        val streamCheckResult = StreamUtil.checkAndCreateTStream(s, initialData.force)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
      case s: KafkaSjStream =>
        try {
          val streamCheckResult = StreamUtil.checkAndCreateKafkaTopic(s, initialData.force)
          streamCheckResult match {
            case Left(err) => errors += err
            case _ =>
          }
        } catch {
          case e: TopicExistsException =>
            val messages = ResourceBundle.getBundle("messages")
            errors += MessageFormat.format(
              messages.getString("rest.streams.create.kafka.cannot"),
              errors.mkString("\n")
            )
        }
      case s: ESSjStream =>
        val streamCheckResult = StreamUtil.checkAndCreateEsStream(s, initialData.force)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
      case s: JDBCSjStream =>
        val streamCheckResult = StreamUtil.checkAndCreateJdbcStream(s, initialData.force)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
    }

    errors
  }


  /**
   * Check t-stream for existence
   * If stream does not exist, it will be created
   * Else compare count of partitions
   *
   * @param stream - T-stream for checking
   * @return - Error, if cannot create stream or stream is incorrect
   */
  def checkAndCreateTStream(stream: TStreamSjStream, force: Boolean = false) = {
    logger.debug(s"Stream ${stream.name}. Check and create t-stream.")
    val service = stream.service.asInstanceOf[TStreamService]
    val metadataProvider = service.metadataProvider
    val hosts = metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toSet
    val cassandraConnectorConf = CassandraConnectorConf.apply(hosts)
    val metadataStorage = (new MetadataStorageFactory).getInstance(conf = cassandraConnectorConf, service.metadataNamespace)

    val dataProvider = service.dataProvider
    var dataStorage: IStorage[Array[Byte]] = null
    if (dataProvider.providerType.equals(ProviderLiterals.cassandraType)) {
      dataStorage = (new cassandra.Factory).getInstance(cassandraConnectorConf, service.dataNamespace)
    } else if (dataProvider.providerType.equals(ProviderLiterals.aerospikeType)) {
      val options = new aerospike.Options(
        service.dataNamespace,
        dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toSet
      )
      dataStorage = (new aerospike.Factory).getInstance(options)
    }

    if (BasicStreamService.isExist(stream.name, metadataStorage)) {
      if (force) {
        BasicStreamService.deleteStream(stream.name, metadataStorage)
        logger.debug(s"Stream ${stream.name}. T-stream is deleted.")

        createTStream(stream, metadataStorage, dataStorage)
        Right(true)
      } else {
        val tStream = BasicStreamService.loadStream[Array[Byte]](
          stream.name,
          metadataStorage,
          dataStorage
        )
        if (tStream.getPartitions != stream.asInstanceOf[TStreamSjStream].partitions) {
          logger.debug(s"T-stream partitions (${stream.asInstanceOf[TStreamSjStream].partitions}) " +
            s"mismatch with partitions of existent t-stream (${tStream.getPartitions}).")
          Left(s"Partitions count of stream ${stream.name} mismatch")
        } else {
          Right(true)
        }
      }
    } else {
      createTStream(stream, metadataStorage, dataStorage)
      Right(true)
    }
  }

  /**
   * @param stream Stream for creating
   * @param metadataStorage t-stream metadata storage (cassandra storage)
   * @param dataStorage t-stream data storage (aerospike or cassandra)
   */
  private def createTStream(stream: TStreamSjStream,
                            metadataStorage: MetadataStorage,
                            dataStorage: IStorage[Array[Byte]]) = {
    val tstreamFactory = new TStreamsFactory()
    val streamTTL = tstreamFactory.getProperty(TSF_Dictionary.Stream.TTL).asInstanceOf[Int]
    BasicStreamService.createStream(
      stream.name,
      stream.asInstanceOf[TStreamSjStream].partitions,
      streamTTL,
      stream.description,
      metadataStorage,
      dataStorage
    )
    logger.debug(s"Stream ${stream.name}. T-stream is created.")
  }

  /**
   * Check kafka topic for existence
   * If kafka topic does not exist, it will be created
   * Else compare count of partitions
   *
   * @param stream - Kafka topic (stream)
   * @return - Error, if topic is incorrect or cannot create it
   */
  def checkAndCreateKafkaTopic(stream: KafkaSjStream, force: Boolean = false) = {
    logger.debug(s"Stream ${stream.name}. Check and create kafka stream.")
    val service = stream.service.asInstanceOf[KafkaService]
    val brokers = service.provider.hosts
    val replications = brokers.length
    val zkHost = service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConfigSettingsUtils.getZkSessionTimeout()
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)
    if (!AdminUtils.topicExists(zkUtils, stream.name)) {
      logger.debug(s"Try creating kafka topic with name ${stream.name}")
      AdminUtils.createTopic(zkUtils, stream.name, stream.asInstanceOf[KafkaSjStream].partitions, replications, new Properties())
      Right(s"Topic ${stream.name} is created")
    } else {
      if (force) {
        try {
          AdminUtils.deleteTopic(zkUtils, stream.name)
          logger.debug(s"Kafka topic ${stream.name} is deleted.")
          AdminUtils.createTopic(zkUtils, stream.name, stream.asInstanceOf[KafkaSjStream].partitions, replications, new Properties())
          logger.debug(s"Kafka topic ${stream.name} is created.")
          Right(s"Topic ${stream.name} is created")
        } catch {
          case ex: TopicAlreadyMarkedForDeletionException =>
            logger.error("Cannot deleting kafka topic", ex)
            Left(s"Cannot deleting kafka topic ${stream.name}")
        }
      } else {
        val topicMetadata = AdminUtils.fetchTopicMetadataFromZk(stream.name, zkUtils)
        if (topicMetadata.partitionsMetadata.size != stream.asInstanceOf[KafkaSjStream].partitions) {
          logger.debug(s"Kafka stream partitions (${stream.asInstanceOf[KafkaSjStream].partitions}) " +
            s"mismatch partitions of exists kafka topic (${topicMetadata.partitionsMetadata.size}).")
          Left(s"Partitions count of stream ${stream.name} mismatch")
        } else {
          logger.debug(s"Kafka topic ${stream.name} already exists.")
          Right(s"Topic ${stream.name} is exists")
        }
      }
    }
  }

  /**
   * Check elasticsearch index for existence
   * If ES index does not exist, it will be created
   *
   * @param stream - ES index (stream)
   * @return - Error, if index is incorrect or cannot create it
   */
  def checkAndCreateEsStream(stream: ESSjStream, force: Boolean = false) = {
    logger.debug(s"Stream ${stream.name}. Check and create elasticsearch stream.")
    val service = stream.service.asInstanceOf[ESService]
    val hosts: Array[InetSocketTransportAddress] = service.provider.hosts.map { s =>
      val parts = s.split(":")
      new InetSocketTransportAddress(new InetSocketAddress(parts(0), parts(1).toInt))
    }
    val client = TransportClient.builder().build().addTransportAddresses(hosts.head)
    if (!client.admin().indices().exists(new IndicesExistsRequest(service.index)).actionGet().isExists) {
      Left(s"Index ${service.index} is not exists")
    } else {
      if (force) {
        deleteEsStream(stream)
      }
      Right(s"Index ${service.index} is exists")
    }
  }

  /**
   * Check sql table for existence
   * If table does not exist, then it will be created
   *
   * @param stream - SQL table (stream)
   * @return - Error, if table is incorrect or cannot creating it
   */
  def checkAndCreateJdbcStream(stream: JDBCSjStream, force: Boolean = false) = {
    logger.debug(s"Stream ${stream.name}. Check and create jdbc stream.")
    val service = stream.service.asInstanceOf[JDBCService]
    //todo add jdbc support
    if (true) {
      Right("yes")
    } else {
      Left("fail")
    }
  }

  /**
   * Generating name of task for stream generator
   *
   * @param stream - SjStream object
   * @return - Task name for transaction generator application
   */
  def createGeneratorTaskName(stream: TStreamSjStream) = {
    var name = ""
    if (stream.generator.generatorType.equals(GeneratorLiterals.perStreamType)) {
      name = s"${stream.generator.service.name}-${stream.name}-tg"
    } else {
      name = s"${stream.generator.service.name}-global-tg"
    }
    name.replaceAll("_", "-")
  }

  /**
   * Delete stream from base
   *
   * @param stream Stream for deleting
   */
  def deleteStream(stream: SjStream) = {
    stream match {
      case s: TStreamSjStream =>
        deleteTStream(stream.asInstanceOf[TStreamSjStream])
      case s: KafkaSjStream =>
        deleteKafkaStream(stream.asInstanceOf[KafkaSjStream])
      case s: ESSjStream =>
        deleteEsStream(stream.asInstanceOf[ESSjStream])
    }
  }

  private def deleteTStream(stream: TStreamSjStream) = {
    val service = stream.service.asInstanceOf[TStreamService]
    val metadataProvider = service.metadataProvider
    val hosts = metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toSet
    val cassandraConnectorConf = CassandraConnectorConf.apply(hosts)
    val metadataStorage = (new MetadataStorageFactory).getInstance(cassandraConnectorConf, service.metadataNamespace)

    val dataProvider = service.dataProvider
    var dataStorage: IStorage[Array[Byte]] = null
    if (dataProvider.providerType.equals(ProviderLiterals.cassandraType)) {
      dataStorage = (new cassandra.Factory).getInstance(cassandraConnectorConf, service.dataNamespace)
    } else if (dataProvider.providerType.equals(ProviderLiterals.aerospikeType)) {
      val options = new Options(
        service.dataNamespace,
        dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toSet
      )
      dataStorage = (new aerospike.Factory).getInstance(options)
    }

    if (BasicStreamService.isExist(stream.name, metadataStorage)) {
      BasicStreamService.deleteStream(stream.name, metadataStorage)
    }
  }

  private def deleteKafkaStream(stream: KafkaSjStream) = {
    logger.info(s"Deleting kafka topic ${stream.name}")
    val service = stream.service.asInstanceOf[KafkaService]
    val zkHost = service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConfigSettingsUtils.getZkSessionTimeout()
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)
    if (AdminUtils.topicExists(zkUtils, stream.name)) {
      try {
        AdminUtils.deleteTopic(zkUtils, stream.name)
        logger.debug(s"Kafka topic ${stream.name} is deleted.")
      } catch {
        case ex: TopicAlreadyMarkedForDeletionException =>
          logger.error("Cannot deleting kafka topic", ex)
      }
    }
  }

  private def deleteEsStream(stream: ESSjStream) = {
    val service = stream.service.asInstanceOf[ESService]
    val hosts: Array[InetSocketTransportAddress] = service.provider.hosts.map { s =>
      val parts = s.split(":")
      new InetSocketTransportAddress(new InetSocketAddress(parts(0), parts(1).toInt))
    }
    val client = TransportClient.builder().build().addTransportAddresses(hosts.head)
    val esRequest: SearchResponse = client
      .prepareSearch(service.index)
      .setTypes(stream.name)
      .setSize(2000)
      .execute()
      .get()
    val outputData = esRequest.getHits

    outputData.getHits.foreach { hit =>
      val id = hit.getId
      client.prepareDelete(service.index, stream.name, id).execute().actionGet()
    }
  }
}
