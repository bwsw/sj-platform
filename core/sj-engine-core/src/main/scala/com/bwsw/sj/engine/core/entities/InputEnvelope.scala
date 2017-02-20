package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals

/**
 * Provides a wrapper for t-stream transaction that is formed by input engine.
 */

class InputEnvelope[T <: AnyRef](var key: String,
                    var outputMetadata: Array[(String, Int)],
                    var duplicateCheck: Boolean,
                    var data: T) extends Envelope {
  streamType = StreamLiterals.inputDummy
}