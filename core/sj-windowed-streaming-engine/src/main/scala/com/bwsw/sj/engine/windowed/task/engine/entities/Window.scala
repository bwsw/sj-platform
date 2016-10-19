package com.bwsw.sj.engine.windowed.task.engine.entities

import scala.collection.mutable.ArrayBuffer

class Window(val slidingInterval: Int) {
  val batches: ArrayBuffer[Batch] = ArrayBuffer()

  def copy() = {
    val copy = new Window(this.slidingInterval)
    this.batches.foreach(x => copy.batches += x)

    copy
  }
}
