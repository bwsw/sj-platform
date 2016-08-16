package com.bwsw.sj.examples.sflow.module.output

import java.util.Date

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.examples.sflow.module.output.data.TrafficMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 * Created: 23/06/2016
 *
 * @author Kseniya Mikhaleva
 */
class SflowOutputExecutor extends OutputStreamingExecutor {
  val jsonSerializer = new JsonSerializer()
  val objectSerializer = new ObjectSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  def onTransaction(envelope: TStreamEnvelope): List[OutputEnvelope] = {
    val list = envelope.data.map { bytes =>
      val data = new TrafficMetrics()
      val rawData = objectSerializer.deserialize(bytes).asInstanceOf[String].split(",")
      data.ts = new Date(rawData(0).toLong)
      data.srcAs = rawData(1).toInt
      data.trafficSum = rawData.last.toLong
      if (rawData.length == 4) {
        data.dstAs = rawData(2)
      }
      val outputEnvelope = new OutputEnvelope
      outputEnvelope.data = data
      outputEnvelope.streamType = "elasticsearch-output"
      outputEnvelope
    }
    list
  }
}

