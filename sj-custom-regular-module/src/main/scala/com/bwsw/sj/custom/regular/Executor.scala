package com.bwsw.sj.custom.regular

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.custom.regular.udf.GeoIp
import com.bwsw.sj.custom.regular.utils.SflowParser
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.StateStorage


class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()
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
        val sflow = SflowParser.parse(kafkaEnvelope.data)
        val srcAs = GeoIp.resolveAs(sflow("srcIP"))
        val dstAs = GeoIp.resolveAs(sflow("dstIP"))
        val prefixAsToAs = s"$srcAs-$dstAs"
        if (!state.isExist(s"traffic-sum-$srcAs")) state.set(s"traffic-sum-$srcAs", 0)
        if (!state.isExist(s"traffic-sum-between-$srcAs-$dstAs")) state.set(s"traffic-sum-between-$srcAs-$dstAs", 0)

        val bandwidth = sflow("packetSize").toInt * sflow("samplingRate").toInt

        var trafficSum = state.get(s"traffic-sum-$srcAs").asInstanceOf[Int]
        trafficSum += bandwidth
        state.set(s"traffic-sum-$srcAs", trafficSum)

        var trafficSumBetweenAs = state.get(s"traffic-sum-between-$prefixAsToAs").asInstanceOf[Int]
        trafficSumBetweenAs += bandwidth
        state.set(s"traffic-sum-between-$prefixAsToAs", trafficSumBetweenAs)

      case tstreamEnvelope: TStreamEnvelope =>
        println("t-stream envelope is received")
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
    val outputForAs = manager.getRoundRobinOutput("src-as-traffic-sum")
    val outputForAsToAs = manager.getRoundRobinOutput("src-as-to-as-traffic-sum")
    val timestamp = System.currentTimeMillis / 1000
    val (sourceAsTrafficSum, sourceAsToAsTrafficSum) = state.getAll.partition(x => !x._1.contains("traffic-sum-between"))
    sourceAsTrafficSum.map(x => timestamp + "," + x._1.replace("traffic-sum-", "") + "," + x._2.toString).foreach(x => outputForAs.put(objectSerializer.serialize(x)))
    sourceAsToAsTrafficSum
      .map(x => timestamp + "," + x._1.replace("traffic-sum-between-", "").split("-").mkString(",") + "," + x._2.toString)
      .foreach(x => outputForAsToAs.put(objectSerializer.serialize(x)))
  }

  override def onIdle(): Unit = {
    println("on Idle")
  }

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