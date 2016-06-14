package com.bwsw.sj.engine.core.entities

/**
 * Provides a wrapper for kafka message.
 */
class KafkaEnvelope() extends Envelope() {
  var data: Array[Byte] = null
  var offset: Long = 0
  streamType = "kafka-stream"

}
