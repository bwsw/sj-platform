package com.bwsw.sj.crud.rest.utils

import java.net.InetSocketAddress
import java.util.Properties

import com.aerospike.client.Host
import com.bwsw.sj.common.DAL.model.{TStreamService, KafkaService, SjStream}
import com.bwsw.tstreams.coordination.Coordinator
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.data.cassandra.{CassandraStorageFactory, CassandraStorageOptions}
import com.bwsw.tstreams.metadata.MetadataStorageFactory
import com.bwsw.tstreams.services.BasicStreamService
import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection
import org.redisson.{Redisson, Config}

/**
  * Created: 19/05/2016
  *
  * @author Kseniya Tomskikh
  */
object StreamUtil {

  /**
    * Check t-stream for exists
    * If stream is not exists, then it created
    * Else compare count of partitions
    * @param stream - T-stream for checking
    * @return - Error, if cannot creating stream or stream is incorrect
    */
  def checkAndCreateTStream(stream: SjStream) = {
    val service = stream.service.asInstanceOf[TStreamService]
    val metadataProvider = service.metadataProvider
    val hosts = metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt))
    val metadataStorage = (new MetadataStorageFactory).getInstance(hosts.toList, service.metadataNamespace)

    val dataProvider = service.dataProvider
    var dataStorage: IStorage[Array[Byte]] = null
    if (dataProvider.providerType.equals("cassandra")) {
      val options = new CassandraStorageOptions(
        dataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList,
        service.dataNamespace
      )
      dataStorage = (new CassandraStorageFactory).getInstance(options)
    } else if (dataProvider.providerType.equals("aerospike")) {
      val options = new AerospikeStorageOptions(
        service.dataNamespace,
        dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toList
      )
      dataStorage = (new AerospikeStorageFactory).getInstance(options)
    }

    val lockProvider = service.lockProvider
    val redisConfig = new Config()
    redisConfig.useSingleServer().setAddress(lockProvider.hosts.head)
    val coordinator = new Coordinator(service.lockNamespace, Redisson.create(redisConfig))

    if (BasicStreamService.isExist(stream.name, metadataStorage)) {
      val tStream = BasicStreamService.loadStream[Array[Byte]](
        stream.name,
        metadataStorage,
        dataStorage,
        coordinator
      )
      if (tStream.getPartitions != stream.partitions) {
        Left(s"Partitions count of stream ${stream.name} mismatch")
      } else {
        Right(true)
      }
    } else {
      BasicStreamService.createStream(
        stream.name,
        stream.partitions,
        5000,
        "", metadataStorage,
        dataStorage,
        coordinator
      )
      Right(true)
    }
  }

  /**
    * Check kafka topic for exists
    * If kafka topic is not exists, then it creating
    * Else compare count of partitions
    * @param stream - Kafka topic (stream)
    * @return - Error, if topic is incorrect or cannot creating it
    */
  def checkAndCreateKafkaTopic(stream: SjStream) = {
    val service = stream.service.asInstanceOf[KafkaService]
    val brokers = service.provider.hosts
    val replications = brokers.length
    val zkHost = "127.0.0.1:2181"//todo
    val zkConnect = new ZkConnection(zkHost)
    val zkClient = ZkUtils.createZkClient(zkHost, 30000, 30000)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)
    if (!AdminUtils.topicExists(zkUtils, stream.name)) {
      AdminUtils.createTopic(zkUtils, stream.name, stream.partitions, replications, new Properties())
      Right(s"Topic ${stream.name} is created")
    } else {
      val topicMetadata = AdminUtils.fetchTopicMetadataFromZk(stream.name, zkUtils)
      if (topicMetadata.partitionsMetadata.size != stream.partitions) {
        Left(s"Partitions count of stream ${stream.name} mismatch")
      } else {
        Right(s"Topic ${stream.name} is exists")
      }
    }
  }

}
