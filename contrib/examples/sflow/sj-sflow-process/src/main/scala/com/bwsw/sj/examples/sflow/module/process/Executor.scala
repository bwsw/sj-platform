package com.bwsw.sj.examples.sflow.module.process

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.utils.{GeoIp, SflowParser}
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.StateStorage


class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor[Array[Byte]](manager) {

  val objectSerializer = new ObjectSerializer()
  val state: StateStorage = manager.getState
  var lastTs: Long = 0

  override def onInit(): Unit = {
    println("onInit")
  }

  override def onAfterCheckpoint(): Unit = {
    println("on after checkpoint")
  }

  override def onMessage(envelope: KafkaEnvelope[Array[Byte]]): Unit = {
    val maybeSflow = SflowParser.parse(envelope.data)
    if (maybeSflow.isDefined) {
      val sflowRecord = maybeSflow.get
      lastTs = sflowRecord.timestamp * 1000
      val srcAs = GeoIp.resolveAs(sflowRecord.srcIP)
      val dstAs = GeoIp.resolveAs(sflowRecord.dstIP)
      val prefixAsToAs = s"$srcAs-$dstAs"
      if (!state.isExist(s"traffic-sum-$srcAs")) state.set(s"traffic-sum-$srcAs", 0L)
      if (!state.isExist(s"traffic-sum-between-$prefixAsToAs")) state.set(s"traffic-sum-between-$prefixAsToAs", 0L)

      val bandwidth = sflowRecord.packetSize * sflowRecord.samplingRate

      var trafficSum = state.get(s"traffic-sum-$srcAs").asInstanceOf[Long]
      trafficSum += bandwidth
      state.set(s"traffic-sum-$srcAs", trafficSum)

      var trafficSumBetweenAs = state.get(s"traffic-sum-between-$prefixAsToAs").asInstanceOf[Long]
      trafficSumBetweenAs += bandwidth
      state.set(s"traffic-sum-between-$prefixAsToAs", trafficSumBetweenAs)
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
    val (sourceAsTrafficSum, sourceAsToAsTrafficSum) = state.getAll.partition(x => !x._1.contains("traffic-sum-between"))
    sourceAsTrafficSum.map(x => lastTs.toString + "," + x._1.replace("traffic-sum-", "") + "," + x._2.toString).foreach(x => outputForAs.put(objectSerializer.serialize(x)))
    sourceAsToAsTrafficSum
      .map(x => lastTs.toString + "," + x._1.replace("traffic-sum-between-", "").split("-").mkString(",") + "," + x._2.toString)
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