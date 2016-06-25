package com.bwsw.sj.module.output.sflow

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.module.output.sflow.data.TrafficMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 * Created: 23/06/2016
 *
 * @author Kseniya Mikhaleva
 */
class SflowOutputHandler extends OutputStreamingHandler {
  val jsonSerializer = new JsonSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  def onTransaction(envelope: TStreamEnvelope): List[OutputEnvelope] = {
    val list = envelope.data.map { bytes =>
      val data = new TrafficMetrics()
      val rawData = new String(bytes).split(",")
      data.ts = rawData(0).toLong
      data.srcAs = rawData(1).toInt
      data.trafficSum = rawData.last.toLong
      if (rawData.length == 4) {
         data.dstAs = rawData(2).toInt
      }
      val outputEnvelope = new OutputEnvelope
      outputEnvelope.data = data
      outputEnvelope.streamType = "elasticsearch-output"
      outputEnvelope
    }
    list
  }
}

