package com.bwsw.sj.stubs.module.output

import java.util.Calendar

import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.output.types.es._
import com.bwsw.sj.stubs.module.output.data.StubEsData

/**
  * Handler for work with t-stream envelopes
  * Executor trait for output-streaming module
  *
  * @author Kseniya Tomskikh
  */
class StubOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[(Integer, String)](manager) {

  /**
    * Transform t-stream transaction to output entities
    *
    * @param envelope Input T-Stream envelope
    * @return List of output envelopes
    */
  override def onMessage(envelope: TStreamEnvelope[(Integer, String)]): Seq[OutputEnvelope] = {
    println("Processed: " + envelope.data.size + " elements")

    val list = envelope.data.dequeueAll(_ => true).map {
      case (i, s) => new StubEsData(Calendar.getInstance().getTime, i, s)
    }

    list
  }

  override def getOutputEntity = {
    val entityBuilder = new ElasticsearchEntityBuilder()
    val entity = entityBuilder
      .field(new DateField("test-date"))
      .field(new IntegerField("value"))
      .field(new JavaStringField("string-value"))
      .build()
    entity
  }
}
