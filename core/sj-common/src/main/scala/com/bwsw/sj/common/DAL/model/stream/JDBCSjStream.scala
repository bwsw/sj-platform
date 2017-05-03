package com.bwsw.sj.common.DAL.model.stream

import com.bwsw.sj.common.DAL.model.service.JDBCService
import com.bwsw.sj.common.rest.entities.stream.JDBCStreamData
import com.bwsw.sj.common.utils.StreamLiterals

class JDBCSjStream(override val name: String,
                   override val description: String,
                   override val service: JDBCService,
                   override val tags: Array[String],
                   override val force: Boolean,
                   val primary: String,
                   override val streamType: String = StreamLiterals.jdbcOutputType)
  extends SjStream(name, description, service, tags, force, streamType) {

  override def asProtocolStream() = {
    val streamData = new JDBCStreamData
    super.fillProtocolStream(streamData)

    streamData.primary = this.primary

    streamData
  }

  override def delete(): Unit = {}
}
