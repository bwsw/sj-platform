package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.rest.model.stream.TStreamStreamApi
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.tstreams.common.StorageClient
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}

class TStreamStreamDomain(override val name: String,
                          override val service: TStreamServiceDomain,
                          val partitions: Int,
                          override val description: String = "No description",
                          override val force: Boolean = false,
                          override val tags: Array[String] = Array(),
                          override val streamType: String = StreamLiterals.tstreamType)
  extends StreamDomain(name, description, service, force, tags, streamType) {

  override def asProtocolStream(): TStreamStreamApi = {
    val streamData = new TStreamStreamApi
    super.fillProtocolStream(streamData)

    streamData.partitions = this.partitions

    streamData
  }

  override def create(): Unit = {
    val factory = new TStreamsFactory()
    factory.setProperty(ConfigurationOptions.StorageClient.Zookeeper.prefix, this.service.prefix)
      .setProperty(ConfigurationOptions.Coordination.endpoints, this.service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, this.service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Auth.key, this.service.token)
    val storageClient: StorageClient = factory.getStorageClient()

    if (!storageClient.checkStreamExists(this.name)) {
      storageClient.createStream(
        this.name,
        this.partitions,
        StreamLiterals.ttl,
        this.description
      )
    }

    storageClient.shutdown()
  }

  override def delete(): Unit = {
    val factory = new TStreamsFactory()
    factory.setProperty(ConfigurationOptions.StorageClient.Zookeeper.prefix, this.service.prefix)
      .setProperty(ConfigurationOptions.Coordination.endpoints, this.service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, this.service.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Auth.key, this.service.token)
    val storageClient = factory.getStorageClient()

    if (storageClient.checkStreamExists(this.name)) {
      storageClient.deleteStream(this.name)
    }

    storageClient.shutdown()
  }
}
