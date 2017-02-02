package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.stream.TStreamSjStreamData

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
    val streamData = new TStreamSjStreamData
    super.fillProtocolStream(streamData)

    streamData.partitions = this.partitions

    streamData
  }

  override def create() = {
    val service = this.service.asInstanceOf[TStreamService]
//    val dataStorage = createDataStorage(service)
//    val metadataStorage = createMetadataStorage(service)
//
//    if (!StreamService.isExist(this.name, metadataStorage)) {
//      StreamService.createStream(
//        this.name,
//        this.partitions,
//        StreamLiterals.ttl,
//        this.description,
//        metadataStorage,
//        dataStorage
//      )
//    }
  }

  override def delete() = {
    val service = this.service.asInstanceOf[TStreamService]
//    val metadataStorage = createMetadataStorage(service)
//    if (StreamService.isExist(this.name, metadataStorage)) {
//      StreamService.deleteStream(this.name, metadataStorage)
//    }
  }
}
