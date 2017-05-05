package com.bwsw.sj.common._dal.model.stream

import com.bwsw.sj.common._dal.model.service.JDBCService
import com.bwsw.sj.common.rest.model.stream.JDBCStreamData
import com.bwsw.sj.common.utils.StreamLiterals

class JDBCSjStream(override val name: String,
                   override val service: JDBCService,
                   val primary: String,
                   override val description: String = "No description",
                   override val force: Boolean = false,
                   override val tags: Array[String] = Array(),
                   override val streamType: String = StreamLiterals.jdbcOutputType)
  extends SjStream(name, description, service, force, tags, streamType) {

  override def asProtocolStream() = {
    val streamData = new JDBCStreamData
    super.fillProtocolStream(streamData)

    streamData.primary = this.primary

    streamData
  }

  override def delete(): Unit = {}
}
