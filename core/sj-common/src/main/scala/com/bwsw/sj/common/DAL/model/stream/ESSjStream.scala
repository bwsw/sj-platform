package com.bwsw.sj.common.DAL.model.stream

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.DAL.model.service.{ESService, Service}
import com.bwsw.sj.common.rest.entities.stream.ESStreamData

class ESSjStream() extends SjStream {
  def this(name: String,
           description: String,
           service: Service,
           streamType: String,
           tags: Array[String]): Unit = {
    this()
    this.name = name
    this.description = description
    this.service = service
    this.streamType = streamType
    this.tags = tags
  }

  override def asProtocolStream(): ESStreamData = {
    val streamData = new ESStreamData
    super.fillProtocolStream(streamData)

    streamData
  }

  override def delete(): Unit = {
    val service = this.service.asInstanceOf[ESService]
    val hosts = service.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)
    client.deleteDocuments(service.index, this.name)

    client.close()
  }
}
