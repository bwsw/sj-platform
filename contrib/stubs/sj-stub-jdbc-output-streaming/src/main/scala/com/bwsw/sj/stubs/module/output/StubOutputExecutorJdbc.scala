package com.bwsw.sj.stubs.module.output

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.output.types.jdbc.{IntegerField, JavaStringField, JdbcEntityBuilder}
import com.bwsw.sj.stubs.module.output.data.StubJdbcData

/**
 * Handler for work with t-stream envelopes
 * Executor trait for output-streaming module
 *
 * @author Diryavkin Dmitry
 */
class StubOutputExecutorJdbc(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[(Integer, String)](manager) {

  val objectSerializer = new ObjectSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope[(Integer, String)]): Seq[OutputEnvelope] = {
    println("Processed: " + envelope.data.size + " elements")

    val list = envelope.data.dequeueAll(_ => true).map {
      case (i, s) =>
        val dataJDBC: StubJdbcData = new StubJdbcData
        dataJDBC.value = i
        dataJDBC.stringValue = s
        dataJDBC
    }
    list
  }

  override def getOutputEntity = {
    val entity = new JdbcEntityBuilder()
      .field(new JavaStringField("id"))
      .field(new IntegerField("value"))
      .field(new JavaStringField("string_value"))
      .build()
    entity
  }
}
