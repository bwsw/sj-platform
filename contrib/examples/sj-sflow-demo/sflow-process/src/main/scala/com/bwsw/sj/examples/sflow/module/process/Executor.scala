package com.bwsw.sj.examples.sflow.module.process

import java.util.Random

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.utils.{GeoIp, SflowRecord}
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.windowed.{WindowRepository, WindowedStreamingExecutor}
import com.bwsw.sj.examples.sflow.module.process.mapreduce.Generator
import com.bwsw.sj.engine.core.entities.TStreamEnvelope

import scala.collection.mutable.ArrayBuffer


class Executor(manager: ModuleEnvironmentManager) extends WindowedStreamingExecutor(manager) {
  private val objectSerializer = new ObjectSerializer()
  private val storage = ArrayBuffer[SflowRecord]()

  override def onWindow(windowRepository: WindowRepository): Unit = {
    val allWindows = windowRepository.getAll()

    val envelopes = allWindows.flatMap(_._2.batches).flatMap(_.envelopes).map(_.asInstanceOf[TStreamEnvelope])
    val sflowRecords = envelopes.flatMap(_.data.map(bytes => {
      val sflowRecord = objectSerializer.deserialize(bytes).asInstanceOf[SflowRecord]
      sflowRecord.srcAs = GeoIp.resolveAs(sflowRecord.srcIP)
      sflowRecord.dstAs = GeoIp.resolveAs(sflowRecord.dstIP)

      sflowRecord
    }))

    storage ++= sflowRecords
  }

  override def onEnter(): Unit = {
    val gen = new Generator()
    gen.putRecords(storage.asInstanceOf[Array[SflowRecord]])

    var output = manager.getRoundRobinOutput("SrcAsStream")
    output.put(objectSerializer.serialize(gen.SrcAsReduceResult()))

    output = manager.getRoundRobinOutput("DstAsStream")
    output.put(objectSerializer.serialize(gen.DstAsReduceResult()))

    output = manager.getRoundRobinOutput("SrcDstStream")
    output.put(objectSerializer.serialize(gen.SrcDstReduceResult()))

    output = manager.getRoundRobinOutput("SrcIpStream")
    output.put(objectSerializer.serialize(gen.SrcIpReduceResult()))

    output = manager.getRoundRobinOutput("DstIpStream")
    output.put(objectSerializer.serialize(gen.DstIpReduceResult()))
  }

}



