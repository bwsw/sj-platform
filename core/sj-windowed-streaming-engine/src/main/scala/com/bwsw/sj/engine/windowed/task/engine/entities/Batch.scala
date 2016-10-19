package com.bwsw.sj.engine.windowed.task.engine.entities

import scala.collection.mutable.ArrayBuffer

class Batch(val stream: String, val tags: Array[String]) {
  val transactions: ArrayBuffer[Transaction] = ArrayBuffer()
  var countOfAppearing: Int = 0

  def copy() = {
    val copy = new Batch(this.stream, this.tags)
    this.transactions.foreach(x => copy.transactions += x)
    copy.countOfAppearing = this.countOfAppearing

    copy
  }
}
