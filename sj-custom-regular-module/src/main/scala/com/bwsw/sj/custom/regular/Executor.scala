package com.bwsw.sj.custom.regular

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.{TStreamEnvelope, Envelope, KafkaEnvelope}
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.StateStorage


class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()
  val state: StateStorage = manager.getState

  override def onInit(): Unit = {
    if (!state.isExist("sum")) state.set("sum", 0)
    println("new init")
  }

  override def onAfterCheckpoint(): Unit = {
    println("on after checkpoint")
  }

  override def onMessage(envelope: Envelope): Unit = {
    val output = manager.getRoundRobinOutput("as-51740")
    var sum = state.get("sum").asInstanceOf[Int]

    envelope match {
      case kafkaEnvelope: KafkaEnvelope =>
        println("element: " +
          objectSerializer.deserialize(kafkaEnvelope.data).asInstanceOf[Int])
        output.put(kafkaEnvelope.data)
        sum += objectSerializer.deserialize(kafkaEnvelope.data).asInstanceOf[Int]
        state.set("sum", sum)
      case tstreamEnvelope: TStreamEnvelope =>
        println("elements: " +
          tstreamEnvelope.data.map(x => objectSerializer.deserialize(x).asInstanceOf[Int]).mkString(","))

        tstreamEnvelope.data.foreach(x => {
          sum += objectSerializer.deserialize(x).asInstanceOf[Int]
        })
        tstreamEnvelope.data.foreach(output.put)
        state.set("sum", sum)
    }
    println("stream type = " + envelope.streamType)
  }

  override def onTimer(jitter: Long): Unit = {
    println("onTimer")
  }

  override def onAfterStateSave(isFull: Boolean): Unit = {
    if (isFull) {
      println("on after full state saving")
    } else println("on after partial state saving")
  }

  override def onBeforeCheckpoint(): Unit = {
    println("on before checkpoint")
  }

  override def onIdle(): Unit = {
    println("on Idle")
  }

  /**
   * Handler triggered before save state
 *
   * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
   */
  override def onBeforeStateSave(isFullState: Boolean): Unit = {
    println("on before state saving")
  }
}
