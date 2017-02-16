package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonIgnore


/**
 * Represents a message envelope that is received by an Executor for each message
 * that is received from a partition of a specific input (kafka, t-stream, elasticsearch, jdbc)
 */

class Envelope extends Serializable {
  protected var streamType: String = null
  var stream: String = null
  var partition: Int = 0
  var tags: Array[String] = Array()
  var id: Long = 0

  @JsonIgnore()
  def isEmpty() = {
    streamType == null
  }
}