package com.bwsw.sj.stubs.module.output

import java.util.Calendar

import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
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
class StubOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[Integer](manager) {

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope[Integer]): List[Envelope] = {
    println("Processed: " + envelope.data.size + " elements")

    val list = envelope.data.map { value =>

      val data: StubEsData = new StubEsData
      data.value = value
      data.testDate = Calendar.getInstance().getTime

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
