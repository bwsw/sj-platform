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


    override def onAfterCheckpoint(): Unit = {
      println("onCheckpoint")
    }

    override def onTxn(transaction: Transaction): Unit = {
      val output = manager.getRoundRobinOutput("s3")
      val state = manager.getState
      var sum = state.get("sum").asInstanceOf[Int]
      transaction.data.foreach(x => output.put(x))
      sum += 1
      state.set("sum", sum)
      println("in run")
    }

    override def onTimer(jitter: Long): Unit = {
      println("onTimer")
    }

  override def onAfterStateSave(isFull: Boolean): Unit = {
    println("onStateSave")
  }

  override def onBeforeCheckpoint(): Unit = ???

  override def onIdle(): Unit = ???

  /**
   * Handler triggered before save state
   * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
   */
  override def onBeforeStateSave(isFullState: Boolean): Unit = ???
}
