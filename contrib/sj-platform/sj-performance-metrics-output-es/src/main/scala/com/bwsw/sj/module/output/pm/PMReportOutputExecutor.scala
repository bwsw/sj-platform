package com.bwsw.sj.module.output.pm

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.output.types.es.{ElasticsearchEntityBuilder, IntegerField, JavaStringField}
import com.bwsw.sj.module.output.pm.data.PerformanceMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 *
 *
 * @author Kseniya Mikhaleva
 */
class PMReportOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[String](manager) {
  val jsonSerializer = new JsonSerializer()
  val objectSerializer = new ObjectSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope[String]): List[Envelope] = {
    val list = envelope.data.map { performanceMetrics =>
      val data: PerformanceMetrics = jsonSerializer.deserialize[PerformanceMetrics](performanceMetrics)

      data
    }
    list
  }

  override def getOutputModule = {
    val entityBuilder = new ElasticsearchEntityBuilder[String]()
    val entity = entityBuilder
      .field(new IntegerField("id", 10))
      .field(new JavaStringField("name", "someString"))
      .build()
    entity
  }
}

