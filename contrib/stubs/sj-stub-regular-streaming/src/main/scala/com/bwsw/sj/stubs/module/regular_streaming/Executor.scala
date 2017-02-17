package com.bwsw.sj.stubs.module.regular_streaming

import java.util.Random

import com.bwsw.sj.common.engine.DefaultEnvelopeDataSerializer
import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.StateStorage

import scala.language.higherKinds

class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor[Int](manager) {

  val objectSerializer = new DefaultEnvelopeDataSerializer[Int]()
  val state: StateStorage = manager.getState
  val outputs = manager.getStreamsByTags(Array("output"))

  override def onInit(): Unit = {
    if (!state.isExist("sum")) state.set("sum", 0)
    println("new init")
  }

  override def onAfterCheckpoint(): Unit = {
    println("on after checkpoint")
  }

  override def onMessage(envelope: TStreamEnvelope[Int]): Unit = {
    val output = manager.getRoundRobinOutput(outputs(new Random().nextInt(outputs.length)))
    var sum = state.get("sum").asInstanceOf[Int]

    if (new Random().nextInt(100) < 5) throw new Exception("it happened")

    println("elements: " + envelope.data.mkString(","))
    envelope.data.foreach(x => sum += x)
    envelope.data.map(objectSerializer.serialize).foreach(output.put)
    state.set("sum", sum)

    println("stream name = " + envelope.stream)
  }

  override def onMessage(envelope: KafkaEnvelope[Int]): Unit = {
    val output = manager.getRoundRobinOutput(outputs(new Random().nextInt(outputs.length)))
    var sum = state.get("sum").asInstanceOf[Int]

    if (new Random().nextInt(100) < 5) throw new Exception("it happened")

    println("element: " + envelope.data)
    output.put(objectSerializer.serialize(envelope.data))
    sum += envelope.data
    state.set("sum", sum)

    println("stream name = " + envelope.stream)
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
