package com.bwsw.sj.module.output.pm

import com.bwsw.common.{ObjectSerializer, JsonSerializer}
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.module.output.pm.data.PerformanceMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 * Created: 23/06/2016
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
  def onMessage(envelope: TStreamEnvelope): List[OutputEnvelope] = {
    val list = envelope.data.map { rawPM =>
      val performanceMetrics = objectSerializer.deserialize(rawPM).asInstanceOf[String]
      val data: PerformanceMetrics = jsonSerializer.deserialize[PerformanceMetrics](performanceMetrics)
      val outputEnvelope = new OutputEnvelope
      outputEnvelope.data = data
      outputEnvelope.streamType = "elasticsearch-output"
      outputEnvelope
    }
    list
  }
}

