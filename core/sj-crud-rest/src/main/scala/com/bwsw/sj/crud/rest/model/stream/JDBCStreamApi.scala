package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.JDBCStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

class JDBCStreamApi(val primary: String,
                    override val name: String,
                    override val service: String,
                    override val tags: Array[String] = Array(),
                    override val force: Boolean = false,
                    override val description: String = RestLiterals.defaultDescription)
  extends StreamApi(StreamLiterals.jdbcOutputType, name, service, tags, force, description) {

  override def to: JDBCStream =
    new JDBCStream(name, service, primary, tags, force, streamType, description)
}
