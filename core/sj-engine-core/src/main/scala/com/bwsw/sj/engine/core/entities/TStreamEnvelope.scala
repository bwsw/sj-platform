package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals

import scala.collection.mutable

/**
 * Provides a wrapper for t-stream transaction.
 */

class TStreamEnvelope[T <: AnyRef](var data: mutable.Queue[T], var consumerName: String) extends Envelope {
  streamType = StreamLiterals.tstreamType
}







