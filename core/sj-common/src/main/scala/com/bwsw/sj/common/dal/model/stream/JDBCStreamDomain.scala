package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.JDBCServiceDomain
import com.bwsw.sj.common.rest.model.stream.JDBCStreamApi
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

class JDBCStreamDomain(override val name: String,
                   override val service: JDBCServiceDomain,
                   val primary: String,
                   override val description: String = RestLiterals.defaultDescription,
                   override val force: Boolean = false,
                   override val tags: Array[String] = Array(),
                   override val streamType: String = StreamLiterals.jdbcOutputType)
  extends StreamDomain(name, description, service, force, tags, streamType) {

  override def asProtocolStream(): JDBCStreamApi =
    new JDBCStreamApi(
      primary = primary,
      name = name,
      service = service.name,
      tags = tags,
      force = force,
      description = description
    )

  override def delete(): Unit = {}
}
