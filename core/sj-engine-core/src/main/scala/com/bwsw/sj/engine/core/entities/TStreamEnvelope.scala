package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals

import scala.collection.mutable

/**
  * Provides a wrapper for t-stream transaction.
  *
  * @param consumerName name of consumer that obtains messages from t-stream
  *                     (used for saving stream offset to recover after failure)
  * @param data         message data
  * @tparam T type of data containing in a message
  */

class TStreamEnvelope[T <: AnyRef](var data: mutable.Queue[T], var consumerName: String) extends Envelope {
  streamType = StreamLiterals.tstreamType
}







