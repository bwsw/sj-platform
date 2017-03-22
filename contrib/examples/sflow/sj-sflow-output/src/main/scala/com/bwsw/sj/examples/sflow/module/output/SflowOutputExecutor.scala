package com.bwsw.sj.examples.sflow.module.output

import java.util.Date

import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.output.types.es._
import com.bwsw.sj.examples.sflow.module.output.data.TrafficMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 *
 *
 * @author Kseniya Mikhaleva
 */
class SflowOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[String](manager) {
  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope[String]): List[OutputEnvelope] = {
    val list = envelope.data.map { s =>
      val data = new TrafficMetrics()
      val rawData = s.split(",")
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

  override def getOutputEntity = {
    val entityBuilder = new ElasticsearchEntityBuilder()
    val entity = entityBuilder
      .field(new DateField("ts"))
      .field(new IntegerField("src-as"))
      .field(new JavaStringField("dst-as"))
      .field(new LongField("sum-of-traffic"))
      .build()
    entity
  }
}

