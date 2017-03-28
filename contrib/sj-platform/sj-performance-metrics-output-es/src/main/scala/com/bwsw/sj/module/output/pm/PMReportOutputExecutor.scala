package com.bwsw.sj.module.output.pm

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.output.types.es._
import com.bwsw.sj.module.output.pm.data.PerformanceMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 *
 *
 * @author Kseniya Mikhaleva
 */
class PMReportOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[String](manager) {
  val objectSerializer = new ObjectSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope[String]): List[OutputEnvelope] = {
    val list = envelope.data.map { performanceMetrics =>
      val data: PerformanceMetrics = JsonSerializer.deserialize[PerformanceMetrics](performanceMetrics)

      data
    }
    list
  }

  override def getOutputEntity = {
    val entityBuilder = new ElasticsearchEntityBuilder()
    val entity = entityBuilder
      .field(new DateField("pm-datetime"))
      .field(new JavaStringField("task-id"))
      .field(new IntegerField("total-input-envelopes"))
      .field(new IntegerField("total-input-elements"))
      .field(new IntegerField("total-input-bytes"))
      .field(new IntegerField("average-size-input-envelope"))
      .field(new IntegerField("max-size-input-envelope"))
      .field(new IntegerField("min-size-input-envelope"))
      .field(new IntegerField("average-size-input-element"))
      .field(new IntegerField("total-output-envelopes"))
      .field(new IntegerField("total-output-bytes"))
      .field(new IntegerField("average-size-output-envelope"))
      .field(new IntegerField("max-size-output-envelope"))
      .field(new IntegerField("min-size-output-envelope"))
      .field(new IntegerField("average-size-output-element"))
      .field(new LongField("total-idle-time"))
      .field(new ObjectField("input-envelopes-per-stream"))
      .field(new ObjectField("input-elements-per-stream"))
      .field(new ObjectField("input-bytes-per-stream"))
      .field(new ObjectField("output-envelopes-per-stream"))
      .field(new ObjectField("output-elements-per-stream"))
      .field(new ObjectField("output-bytes-per-stream"))
      .field(new IntegerField("state-variables-number"))
      .field(new JavaStringField("input-stream-name"))
      .field(new JavaStringField("output-stream-name"))
      .build()
    entity
  }
}

