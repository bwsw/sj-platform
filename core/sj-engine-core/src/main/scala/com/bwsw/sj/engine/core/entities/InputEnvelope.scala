package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.StreamConstants

/**
 * Provides a wrapper for t-stream transactions (if outputMetadata has more than one element) that is formed by input engine.
 *
 * @author Kseniya Mikhaleva
 */

class InputEnvelope(var key: String,
                    var outputMetadata: Array[(String, Int)],
                    var duplicateCheck: Boolean,
                    var data: Array[Byte]) extends Envelope {
  streamType = StreamConstants.input
}