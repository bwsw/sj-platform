package com.bwsw.sj.module.output.pm

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.module.output.pm.data.PerformanceMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 *
 *
 * @author Kseniya Mikhaleva
 */
class PMReportOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor(manager) {
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

      data
    }
    list
  }
}

