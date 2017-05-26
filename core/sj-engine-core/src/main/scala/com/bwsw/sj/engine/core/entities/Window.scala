package com.bwsw.sj.engine.core.entities

import com.bwsw.sj.common.utils.EngineLiterals

import scala.collection.mutable.ArrayBuffer

/**
  * Used in [[EngineLiterals.batchStreamingType]] engine to collect batches [[Batch]] for each stream
  *
  * @param stream stream name for which batches are collected
  */
class Window(val stream: String) {
  val batches: ArrayBuffer[Batch] = ArrayBuffer()

  def copy(): Window = {
    val copy = new Window(this.stream)
    this.batches.foreach(x => copy.batches += x)

    copy
  }
}
