package com.bwsw.sj.engine.core.entities

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.utils._

/**
  * Used in [[EngineLiterals.batchStreamingType]] engine to collect envelopes [[Envelope]] for each stream
  *
  * @param stream provides envelopes
  * @param tags   for user convenience to realize what kind of data are in a stream
  */
class Batch(val stream: String, val tags: Array[String]) extends Serializable {
  val envelopes: ArrayBuffer[Envelope] = ArrayBuffer()

  def copy(): Batch = {
    val copy = new Batch(this.stream, this.tags)
    this.envelopes.foreach(x => copy.envelopes += x)

    copy
  }
}