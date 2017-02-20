package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals

/**
  * Provides a wrapper for kafka message.
  */

class KafkaEnvelope[T <: AnyRef](var data: T) extends Envelope {
  streamType = StreamLiterals.kafkaStreamType
}
