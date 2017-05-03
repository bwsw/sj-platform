package com.bwsw.sj.common.DAL.model.stream

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.DAL.model.service.ESService
import com.bwsw.sj.common.rest.entities.stream.ESStreamData
import com.bwsw.sj.common.utils.StreamLiterals

class ESSjStream(override val name: String,
                 override val description: String,
                 override val service: ESService,
                 override val tags: Array[String],
                 override val force: Boolean,
                 override val streamType: String = StreamLiterals.esOutputType)
  extends SjStream(name, description, service, tags, force, streamType) {

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
