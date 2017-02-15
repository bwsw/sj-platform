package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals
import scala.reflect.runtime.universe._

/**
 * Provides a wrapper for t-stream transaction.
 */

class TStreamEnvelope[T: TypeTag](var data: List[T], var consumerName: String) extends Envelope {
  streamType = StreamLiterals.tstreamType
}







