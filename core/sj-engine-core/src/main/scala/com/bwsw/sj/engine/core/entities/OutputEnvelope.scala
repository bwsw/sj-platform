package com.bwsw.sj.engine.core.entities

/**
 * Represents a message that is received from an OutputExecutor for each input message
 * that is received from a partition of a t-stream input
 */

class OutputEnvelope extends Envelope {
  var data: OutputEntity = null
}
