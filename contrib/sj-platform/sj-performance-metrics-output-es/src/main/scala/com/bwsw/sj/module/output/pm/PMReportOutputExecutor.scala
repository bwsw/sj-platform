package com.bwsw.sj.module.output.pm

import com.bwsw.common.{ObjectSerializer, JsonSerializer}
import com.bwsw.sj.engine.core.entities.{EsEnvelope, Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.module.output.pm.data.PerformanceMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 *
 *
 * @author Kseniya Mikhaleva
 */
class PMReportOutputExecutor extends OutputStreamingExecutor {
  val jsonSerializer = new JsonSerializer()
  val objectSerializer = new ObjectSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope): List[Envelope] = {
    val list = envelope.data.map { rawPM =>
      val performanceMetrics = objectSerializer.deserialize(rawPM).asInstanceOf[String]
      val data: PerformanceMetrics = jsonSerializer.deserialize[PerformanceMetrics](performanceMetrics)
      val outputEnvelope = new EsEnvelope
      outputEnvelope.data = data
      outputEnvelope
    }
    list
  }
}

