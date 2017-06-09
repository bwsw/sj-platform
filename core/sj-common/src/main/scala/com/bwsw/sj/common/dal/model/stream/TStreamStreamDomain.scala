package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.bwsw.tstreams.storage.StorageClient

/**
  * protected methods and variables need for testing purposes
  */
class TStreamStreamDomain(override val name: String,
                          override val service: TStreamServiceDomain,
                          val partitions: Int,
                          override val description: String = RestLiterals.defaultDescription,
                          override val force: Boolean = false,
                          override val tags: Array[String] = Array())
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.tstreamType) {

  private val factory = new TStreamsFactory()
  factory.setProperty(ConfigurationOptions.Coordination.path, this.service.prefix)
    .setProperty(ConfigurationOptions.Coordination.endpoints, this.service.provider.getConcatenatedHosts())
    .setProperty(ConfigurationOptions.Common.authenticationKey, this.service.token)

  protected def createClient(): StorageClient = factory.getStorageClient()

  override def create(): Unit = {
    val storageClient: StorageClient = createClient()

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
