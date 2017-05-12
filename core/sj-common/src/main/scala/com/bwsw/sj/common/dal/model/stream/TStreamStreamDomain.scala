package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.bwsw.tstreams.common.StorageClient
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}


class TStreamStreamDomain(override val name: String,
                          override val service: TStreamServiceDomain,
                          val partitions: Int,
                          override val description: String = RestLiterals.defaultDescription,
                          override val force: Boolean = false,
                          override val tags: Array[String] = Array())
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.tstreamType) {

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
}
