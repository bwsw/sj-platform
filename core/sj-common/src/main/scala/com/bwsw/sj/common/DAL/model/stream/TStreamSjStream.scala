package com.bwsw.sj.common.DAL.model.stream

import com.bwsw.sj.common.DAL.model.service.TStreamService
import com.bwsw.sj.common.rest.entities.stream.TStreamStreamData
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.tstreams.common.StorageClient
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}

class TStreamSjStream(override val name: String,
                      override val service: TStreamService,
                      val partitions: Int,
                      override val description: String = "No description",
                      override val force: Boolean = false,
                      override val tags: Array[String] = Array(),
                      override val streamType: String = StreamLiterals.tstreamType)
  extends SjStream(name, description, service, force, tags, streamType) {

  override def asProtocolStream() = new TStreamStreamData(
    name,
    service.name,
    tags,
    force,
    description,
    partitions)

  override def create() = {
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

  override def delete() = {
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
