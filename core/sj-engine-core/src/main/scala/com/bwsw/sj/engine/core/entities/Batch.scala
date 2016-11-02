package com.bwsw.sj.engine.core.entities

import scala.collection.mutable.ArrayBuffer

class Batch(val stream: String, val tags: Array[String]) extends Serializable {
  val envelopes: ArrayBuffer[Envelope] = ArrayBuffer()

  def copy() = {
    val copy = new Batch(this.stream, this.tags)
    this.envelopes.foreach(x => copy.envelopes += x)

    copy
  }
}