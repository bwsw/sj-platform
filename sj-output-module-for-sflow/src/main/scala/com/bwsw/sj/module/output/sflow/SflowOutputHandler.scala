package com.bwsw.sj.module.output.sflow

import java.util.Date

import com.bwsw.common.{ObjectSerializer, JsonSerializer}
import com.bwsw.sj.engine.core.entities.{EsEntity, OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.module.output.sflow.data.{TrafficMetricsForSrcAs, TrafficMetricsForPerAs}

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 * Created: 23/06/2016
 *
 * @author Kseniya Mikhaleva
 */
class SflowOutputHandler extends OutputStreamingHandler {
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
      var data: EsEntity = null
      val rawData = objectSerializer.deserialize(bytes).asInstanceOf[String].split(",")
      println(rawData(0))
      if (rawData.length == 4) {
        val trafficMetrics = new TrafficMetricsForPerAs()
        trafficMetrics.ts = new Date(rawData(0).toLong)
        trafficMetrics.srcAs = rawData(1).toInt
        trafficMetrics.trafficSum = rawData.last.toLong
        trafficMetrics.dstAs = rawData(2).toInt
        data = trafficMetrics
      } else {
        val trafficMetrics = new TrafficMetricsForSrcAs()
        trafficMetrics.ts = new Date(rawData(0).toLong)
        trafficMetrics.srcAs = rawData(1).toInt
        trafficMetrics.trafficSum = rawData.last.toLong
        data = trafficMetrics
      }
      val outputEnvelope = new OutputEnvelope
      outputEnvelope.data = data
      outputEnvelope.streamType = "elasticsearch-output"
      outputEnvelope
    }
    list
  }
}

