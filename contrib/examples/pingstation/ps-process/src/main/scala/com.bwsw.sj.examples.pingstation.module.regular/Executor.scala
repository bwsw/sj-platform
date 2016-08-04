package com.bwsw.sj.examples.pingstation.module.regular

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.StateStorage
import com.bwsw.sj.examples.pingstation.module.regular.entities.PingStateVariable


class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()
  val jsonSerializer = new JsonSerializer()
  val state: StateStorage = manager.getState

  override def onInit(): Unit = {
    println("onInit")
  }

  override def onAfterCheckpoint(): Unit = {
    println("on after checkpoint")
  }

  override def onMessage(envelope: Envelope): Unit = {

    envelope match {
      case kafkaEnvelope: KafkaEnvelope =>
        println("kafka envelope is received. It's something strange")

      case tstreamEnvelope: TStreamEnvelope =>

        val fpingResponses = tstreamEnvelope.data
          .map(objectSerializer.deserialize)
          .map(_.asInstanceOf[String])

        tstreamEnvelope.stream match {
          case "echo-response" =>
            val echoResponses = fpingResponses.map(jsonSerializer.deserialize[EchoResponse])

            echoResponses.foreach(x => {
              println(x) //todo for testing

              if (!state.isExist(x.ip)) state.set(x.ip, PingStateVariable(0, 0, 0, 0))

              val pingStateVariable = state.get(x.ip).asInstanceOf[PingStateVariable]

              pingStateVariable.ts = x.ts
              pingStateVariable.totalTime += x.time
              pingStateVariable.totalOk += 1

              state.set(x.ip, pingStateVariable)
            })

          case "unreachable-response" =>
            val unreachableResponses = fpingResponses.map(jsonSerializer.deserialize[UnreachableResponse])

            unreachableResponses.foreach(x => {
              println(x) //todo for testing

              if (!state.isExist(x.ip)) state.set(x.ip, PingStateVariable(0, 0, 0, 0))

              val pingStateVariable = state.get(x.ip).asInstanceOf[PingStateVariable]

              pingStateVariable.ts = x.ts
              pingStateVariable.totalUnreachable += 1

              state.set(x.ip, pingStateVariable)
            })
        }
    }
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
    val outputName = manager.getStreamsByTags(Array("echo", "output")).head
    val output = manager.getRoundRobinOutput(outputName)
    val pingStateVariables = state.getAll.map(x => (x._1, x._2.asInstanceOf[PingStateVariable]))
    pingStateVariables.map(x =>
      x._2.ts + ","
      + x._1 + ","
      + {if (x._2.totalOk != 0 || x._2.totalTime != 0) x._2.totalTime / x._2.totalOk else 0} + ","
      + x._2.totalOk + ","
      + x._2.totalUnreachable
    ).foreach(x => {
      println(x) //todo for testing
      output.put(objectSerializer.serialize(x))
    })
  }

  override def onIdle(): Unit = {}

  /**
   * Handler triggered before persisting a state
   *
   * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
   */
  override def onBeforeStateSave(isFullState: Boolean): Unit = {
    println("on before state saving")
    state.clear()
  }
}