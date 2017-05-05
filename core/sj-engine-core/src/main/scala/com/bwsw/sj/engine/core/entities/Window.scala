package com.bwsw.sj.engine.core.entities

import scala.collection.mutable.ArrayBuffer

class Window(val stream: String) {
  val batches: ArrayBuffer[Batch] = ArrayBuffer()

  def copy(): Window = {
    val copy = new Window(this.stream)
    this.batches.foreach(x => copy.batches += x)

    copy
  }
}
