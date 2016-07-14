package com.bwsw.sj.engine.core.entities

import java.util.UUID

import com.bwsw.sj.common.StreamConstants

/**
 * Provides a wrapper for t-stream transaction.
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class TStreamEnvelope() extends Envelope() {
  var txnUUID: UUID = null
  var consumerName: String = null
  var data: List[Array[Byte]] = null
  streamType = StreamConstants.tStream
}






