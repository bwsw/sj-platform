package com.bwsw.sj.stub

import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.sj.common.module.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.module.regular.RegularStreamingExecutor

class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

    override def init(): Unit = {
      try {
        val state = manager.getState

        println(s"getting state")
        state.set("sum", 0)
      } catch {
        case _: java.lang.Exception => println("exception")
      }

      println("new init")
    }

    override def finish(): Unit = ???

    override def onAfterCheckpoint(): Unit = {
      println("onCheckpoint")
    }

    override def run(transaction: Transaction): Unit = {
      val output = manager.getRoundRobinOutput("s3")
      val state = manager.getState
      var sum = state.get("sum").asInstanceOf[Int]
      transaction.data.foreach(x => output.put(x))
      sum += 1
      state.set("sum", sum)
      println("in run")
    }

    override def onTimer(): Unit = {
      println("onTimer")
    }

  override def onAfterStateSave(isFull: Boolean): Unit = {
    println("onStateSave")
  }
}
