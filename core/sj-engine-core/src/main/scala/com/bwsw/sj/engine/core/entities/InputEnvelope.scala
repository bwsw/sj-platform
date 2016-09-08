package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamConstants

/**
 * Provides a wrapper for t-stream transaction that is formed by input engine.
 */

class InputEnvelope(var key: String,
                    var outputMetadata: Array[(String, Int)],
                    var duplicateCheck: Boolean,
                    var data: Array[Byte]) extends Envelope {
  streamType = StreamConstants.inputDummy
}