package com.bwsw.sj.stub

import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.sj.common.module.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.module.regular.RegularStreamingExecutor

class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

    override def init(): Unit = {
      println("init")
    }

    override def finish(): Unit = ???

    override def onCheckpoint(): Unit = {
      println("onCheckpoint")
    }

    override def run(transaction: Transaction): Unit = {
      val output = manager.getOutput("s3")
      manager.setTimer(1000)
      transaction.data.foreach(x => output += x)
      println("in run")
    }

    override def onTimer(): Unit = {
      println("onTimer")
    }
}
