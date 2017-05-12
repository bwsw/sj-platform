package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream.TStreamStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

class TStreamStreamApi(override val name: String,
                       override val service: String,
                       override val tags: Array[String] = Array(),
                       override val force: Boolean = false,
                       override val description: String = RestLiterals.defaultDescription,
                       val partitions: Int = Int.MinValue)
  extends StreamApi(StreamLiterals.tstreamType, name, service, tags, force, description) {

  override def to: TStreamStream =
    new TStreamStream(name, service, partitions, tags, force, streamType, description)
}
