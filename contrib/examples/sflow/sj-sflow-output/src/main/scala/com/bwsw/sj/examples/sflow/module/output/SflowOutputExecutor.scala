package com.bwsw.sj.examples.sflow.module.output

import java.util.Date

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.examples.sflow.module.output.data.TrafficMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 *
 *
 * @author Kseniya Mikhaleva
 */
class SflowOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor(manager) {
  val jsonSerializer = new JsonSerializer()
  val objectSerializer = new ObjectSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope): List[Envelope] = {
    val list = envelope.data.map { bytes =>
      val data = new TrafficMetrics()
      val rawData = objectSerializer.deserialize(bytes).asInstanceOf[String].split(",")
      data.ts = new Date(rawData(0).toLong)
      data.srcAs = rawData(1).toInt
      data.trafficSum = rawData.last.toLong
      if (rawData.length == 4) {
        data.dstAs = rawData(2)
      }

      data
    }
    list
  }
}

