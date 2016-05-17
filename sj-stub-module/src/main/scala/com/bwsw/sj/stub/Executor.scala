package com.bwsw.sj.stub

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.module.entities.{TStreamEnvelope, Envelope}
import com.bwsw.sj.common.module.environment.ModuleEnvironmentManager
import com.bwsw.sj.common.module.regular.RegularStreamingExecutor


class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()

  override def init(): Unit = {
    println("new init")
  }

  override def onAfterCheckpoint(): Unit = {
    println("on after checkpoint")
  }

  override def onMessage(envelope: Envelope): Unit = {
    val output = manager.getRoundRobinOutput("test_tstream2")
    //var elementCount = state.get("elementCount").asInstanceOf[Int]
    //    var txnCount = state.get(transaction.txnUUID.toString).asInstanceOf[Int]
    //elementCount += transaction.data.length
    if (scala.util.Random.nextInt(100) < 20) throw new Exception("it happened")
    val tStreamEnvelope = envelope.asInstanceOf[TStreamEnvelope]
    println("length = " + tStreamEnvelope.data.length + ", elements: " +
      tStreamEnvelope.data.map(x => objectSerializer.deserialize(x).asInstanceOf[Int]).mkString(","))
    tStreamEnvelope.data.foreach(output.put)
    println("stream type = " + envelope.streamType)
    //state.set("elementCount", elementCount)
  }

  override def onTimer(jitter: Long): Unit = {
    println("onTimer")
  }

  override def onAfterStateSave(isFull: Boolean): Unit = {
    if (isFull) {}
  }

  override def onBeforeCheckpoint(): Unit = {
    println("on before checkpoint")
  }

  override def onIdle(): Unit = {
    println("on Idle")
  }

  /**
   * Handler triggered before save state
   * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
   */
  override def onBeforeStateSave(isFullState: Boolean): Unit = {
    println("on before state save")
  }
}
