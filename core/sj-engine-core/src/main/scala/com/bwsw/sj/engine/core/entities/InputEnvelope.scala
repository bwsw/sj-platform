package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}

/**
  * Provides a wrapper for t-stream transaction that is formed by [[EngineLiterals.inputStreamingType]] engine.
  *
  * @param key            a key for check on duplicate
  * @param outputMetadata information (stream -> partition) - where data should be placed
  * @param duplicateCheck whether a message should be checked on duplicate or not
  * @param data           message data
  * @tparam T type of data containing in a message
  */

class InputEnvelope[T <: AnyRef](var key: String,
                                 var outputMetadata: Array[(String, Int)],
                                 var duplicateCheck: Boolean,
                                 var data: T) extends Envelope {
  streamType = StreamLiterals.inputDummy
}