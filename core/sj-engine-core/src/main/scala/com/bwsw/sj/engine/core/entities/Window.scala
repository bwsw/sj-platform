package com.bwsw.sj.engine.core.entities

import scala.collection.mutable.ArrayBuffer

class Window(val stream: String, val slidingInterval: Int) {
  val batches: ArrayBuffer[Batch] = ArrayBuffer()

  def copy() = {
    val copy = new Window(this.stream, this.slidingInterval)
    this.batches.foreach(x => copy.batches += x)

    copy
  }
}
