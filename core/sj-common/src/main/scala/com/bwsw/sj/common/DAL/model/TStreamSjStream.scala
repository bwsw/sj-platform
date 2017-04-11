package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.stream.TStreamStreamData
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.tstreams.common.StorageClient
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}

class TStreamSjStream() extends SjStream {
  var partitions: Int = 0

  def this(name: String,
           description: String,
           partitions: Int,
           service: Service,
           streamType: String,
           tags: Array[String]) = {
    this()
    this.name = name
    this.description = description
    this.partitions = partitions
    this.service = service
    this.streamType = streamType
    this.tags = tags
  }

  override def asProtocolStream() = {
    val streamData = new TStreamStreamData
    super.fillProtocolStream(streamData)

    streamData.partitions = this.partitions

    streamData
  }

  override def create() = {
    val tStreamService = this.service.asInstanceOf[TStreamService]
    val factory = new TStreamsFactory()
    factory.setProperty(ConfigurationOptions.Coordination.prefix, tStreamService.prefix)
      .setProperty(ConfigurationOptions.Coordination.endpoints, tStreamService.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, tStreamService.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.Stream.name, name)
      .setProperty(ConfigurationOptions.StorageClient.Auth.key, tStreamService.token)
    val storageClient: StorageClient = factory.getStorageClient()

    if (!storageClient.checkStreamExists(this.name)) {
      storageClient.createStream(
        this.name,
        this.partitions,
        StreamLiterals.ttl,
        this.description
      )
    }
  }

  override def delete() = {
    val tStreamService = this.service.asInstanceOf[TStreamService]
    val factory = new TStreamsFactory()
    factory.setProperty(ConfigurationOptions.Coordination.prefix, tStreamService.prefix)
      .setProperty(ConfigurationOptions.Coordination.endpoints, tStreamService.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, tStreamService.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.Stream.name, name)
      .setProperty(ConfigurationOptions.StorageClient.Auth.key, tStreamService.token)
    val storageClient = factory.getStorageClient()

    if (storageClient.checkStreamExists(this.name)) {
      storageClient.deleteStream(this.name)
    }
  }
}
