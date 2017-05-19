package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals

/**
  * Provides a wrapper for kafka message.
  *
  * @param data message data
  * @tparam T type of data containing in a message
  */

class KafkaEnvelope[T <: AnyRef](var data: T) extends Envelope {
  streamType = StreamLiterals.kafkaStreamType
}
