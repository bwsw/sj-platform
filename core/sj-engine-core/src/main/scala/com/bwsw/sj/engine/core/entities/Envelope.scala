package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.bwsw.sj.common.utils.StreamLiterals
/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input stream [[StreamLiterals.types]]
 */

class Envelope extends Serializable {
  protected var streamType: String = _
  var stream: String = _
  var partition: Int = 0
  var tags: Array[String] = Array()
  var id: Long = 0

  @JsonIgnore()
  def isEmpty(): Boolean = Option(streamType).isEmpty
}