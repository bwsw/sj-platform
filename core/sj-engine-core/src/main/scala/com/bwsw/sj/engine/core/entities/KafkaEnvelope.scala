package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals

/**
 * Provides a wrapper for kafka message.
 */

class KafkaEnvelope() extends Envelope() {
  var data: Array[Byte] = null
  var offset: Long = 0
  streamType = StreamLiterals.kafkaStreamType
}
