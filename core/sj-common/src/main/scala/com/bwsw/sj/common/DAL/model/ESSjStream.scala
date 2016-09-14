package com.bwsw.sj.common.DAL.model

import com.bwsw.common.ElasticsearchClient
import com.bwsw.sj.common.rest.entities.stream.ESSjStreamData

class ESSjStream() extends SjStream {
  def this(name: String,
           description: String,
           service: Service,
           streamType: String,
           tags: Array[String]) = {
    this()
    this.name = name
    this.description = description
    this.service = service
    this.streamType = streamType
    this.tags = tags
  }

  override def asProtocolStream() = {
    val streamData = new ESSjStreamData
    super.fillProtocolStream(streamData)

    streamData
  }

  override def delete() = {
    val service = this.service.asInstanceOf[ESService]
    val hosts = service.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)
    val outputData = client.search(service.index, this.name)

    outputData.getHits.foreach { hit =>
      val id = hit.getId
      client.deleteIndexDocumentById(service.index, this.name, id)
    }
  }
}
