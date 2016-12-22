package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals

/**
 * Provides a wrapper for t-stream transaction.
 */

class TStreamEnvelope() extends Envelope() {
  var id: Long = 0
  var consumerName: String = null
  var data: List[Array[Byte]] = List()
  streamType = StreamLiterals.tstreamType
}







