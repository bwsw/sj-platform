package com.bwsw.sj.engine.core.entities

import java.util.UUID

import com.bwsw.sj.common.utils.StreamLiterals

/**
 * Provides a wrapper for t-stream transaction.
 */

class TStreamEnvelope() extends Envelope() {
  var txnUUID: UUID = null
  var consumerName: String = null
  var data: List[Array[Byte]] = null
  streamType = StreamLiterals.tStreamType
}







