package com.bwsw.sj.stubs.module.output

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.stubs.module.output.data.StubJdbcData

/**
 * Handler for work with t-stream envelopes
 * Executor trait for output-streaming module
 *
 * @author Diryavkin Dmitry
 */
class StubOutputExecutorJdbc(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[Array[Byte]](manager) {

  val objectSerializer = new ObjectSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope[Array[Byte]]): List[Envelope] = {
    val list = envelope.data.map { row =>
      val value = objectSerializer.deserialize(row).asInstanceOf[Int]

      val dataJDBC: StubJdbcData = new StubJdbcData
      dataJDBC.value = value
      dataJDBC
    }

    list
  }
}
