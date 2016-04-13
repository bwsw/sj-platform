package com.bwsw.sj.stub

import com.bwsw.sj.common.module.ModuleEnvironmentManager
import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.sj.common.module.regular.RegularStreamingExecutor
import scala.collection._

class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {
  override def init(): Unit = {
    manager.setState(mutable.Map("sumElement" -> 0))
  }

  override def finish(): Unit = ???

  override def onCheckpoint(): Unit = {
    println(s"Checkpoint done! Sum of element = ${manager.getState()("sum")}")
  }

  override def run(transaction: Transaction): Unit = {
    val state = manager.getState()
    state("sum") = state("sum").asInstanceOf[Int] + transaction.data.length
  }

  override def onTimer(): Unit = ???
}
