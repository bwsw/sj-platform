package com.bwsw.sj.common.DAL.model

import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.sj.common.rest.entities.stream.TStreamSjStreamData
import com.bwsw.sj.common.utils.{ProviderLiterals, StreamLiterals}
import com.bwsw.tstreams.common.CassandraConnectorConf
import com.bwsw.tstreams.data.{IStorage, aerospike, cassandra}
import com.bwsw.tstreams.metadata.MetadataStorageFactory
import com.bwsw.tstreams.services.BasicStreamService
import org.mongodb.morphia.annotations.Embedded

class TStreamSjStream() extends SjStream {
  var partitions: Int = 0
  @Embedded var generator: Generator = null

  def this(name: String,
           description: String,
           partitions: Int,
           service: Service,
           streamType: String,
           tags: Array[String],
           generator: Generator) = {
    this()
    this.name = name
    this.description = description
    this.partitions = partitions
    this.service = service
    this.streamType = streamType
    this.tags = tags
    this.generator = generator
  }

  override def asProtocolStream() = {
    val streamData = new TStreamSjStreamData
    super.fillProtocolStream(streamData)

    streamData.partitions = this.partitions
    streamData.generator = this.generator.asProtocolGenerator()

    streamData
  }

  override def create() = {
    val dataStorage = createDataStorage()
    val metadataStorage = createMetadataStorage()

    if (!BasicStreamService.isExist(this.name, metadataStorage)) {
      BasicStreamService.createStream(
        this.name,
        this.partitions,
        StreamLiterals.ttl,
        this.description,
        metadataStorage,
        dataStorage
      )
    }
  }

  override def delete() = {
    val metadataStorage = createMetadataStorage()
    if (BasicStreamService.isExist(this.name, metadataStorage)) {
      BasicStreamService.deleteStream(this.name, metadataStorage)
    }
  }

  private def createMetadataStorage() = {
    val service = this.service.asInstanceOf[TStreamService]
    val metadataProvider = service.metadataProvider
    val hosts = metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toSet
    val cassandraConnectorConf = CassandraConnectorConf.apply(hosts)
    val metadataStorage = (new MetadataStorageFactory).getInstance(cassandraConnectorConf, service.metadataNamespace)

    metadataStorage
  }

  private def createDataStorage() = {
    val service = this.service.asInstanceOf[TStreamService]
    val dataProvider = service.dataProvider
    var dataStorage: IStorage[Array[Byte]] = null
    dataProvider.providerType match {
      case ProviderLiterals.cassandraType =>
        val hosts = dataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toSet
        val cassandraConnectorConf = CassandraConnectorConf.apply(hosts)
        dataStorage = (new cassandra.Factory).getInstance(cassandraConnectorConf, service.dataNamespace)
      case ProviderLiterals.aerospikeType =>
        val options = new aerospike.Options(
          service.dataNamespace,
          dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toSet
        )
        dataStorage = (new aerospike.Factory).getInstance(options)
    }

    dataStorage
  }
}
