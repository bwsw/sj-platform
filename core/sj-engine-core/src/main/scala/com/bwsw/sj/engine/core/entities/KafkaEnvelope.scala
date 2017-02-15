package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals
import scala.reflect.runtime.universe._

/**
  * Provides a wrapper for kafka message.
  */

class KafkaEnvelope[T: TypeTag](var data: T) extends Envelope {
  streamType = StreamLiterals.kafkaStreamType
}
