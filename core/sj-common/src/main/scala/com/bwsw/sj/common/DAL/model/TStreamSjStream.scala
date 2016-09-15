package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.stream.TStreamSjStreamData
import com.bwsw.sj.common.utils._
import com.bwsw.tstreams.services.BasicStreamService
import org.mongodb.morphia.annotations.Embedded
import SjStreamUtilsForCreation._

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
    val service = this.service.asInstanceOf[TStreamService]
    val dataStorage = createDataStorage(service)
    val metadataStorage = createMetadataStorage(service)

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
    val service = this.service.asInstanceOf[TStreamService]
    val metadataStorage = createMetadataStorage(service)
    if (BasicStreamService.isExist(this.name, metadataStorage)) {
      BasicStreamService.deleteStream(this.name, metadataStorage)
    }
  }
}
