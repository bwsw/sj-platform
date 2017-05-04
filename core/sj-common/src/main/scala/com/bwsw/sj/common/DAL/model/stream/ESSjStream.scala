package com.bwsw.sj.common.DAL.model.stream

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.DAL.model.service.ESService
import com.bwsw.sj.common.rest.DTO.stream.ESStreamData
import com.bwsw.sj.common.utils.StreamLiterals

class ESSjStream(override val name: String,
                 override val service: ESService,
                 override val description: String = "No description",
                 override val force: Boolean = false,
                 override val tags: Array[String] = Array(),
                 override val streamType: String = StreamLiterals.esOutputType)
  extends SjStream(name, description, service, force, tags, streamType) {

  override def asProtocolStream(): ESStreamData = {
    val streamData = new ESStreamData
    super.fillProtocolStream(streamData)

    streamData
  }

  override def delete(): Unit = {
    val hosts = this.service.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)
    client.deleteDocuments(this.service.index, this.name)

    client.close()
  }
}
