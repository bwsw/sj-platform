package com.bwsw.sj.common.dal.model.stream

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.dal.model.service.ESServiceDomain
import com.bwsw.sj.common.rest.model.stream.ESStreamApi
import com.bwsw.sj.common.utils.StreamLiterals

class ESStreamDomain(override val name: String,
                     override val service: ESServiceDomain,
                     override val description: String = "No description",
                     override val force: Boolean = false,
                     override val tags: Array[String] = Array(),
                     override val streamType: String = StreamLiterals.esOutputType)
  extends StreamDomain(name, description, service, force, tags, streamType) {

  override def asProtocolStream(): ESStreamApi = {
    val streamData = new ESStreamApi
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
