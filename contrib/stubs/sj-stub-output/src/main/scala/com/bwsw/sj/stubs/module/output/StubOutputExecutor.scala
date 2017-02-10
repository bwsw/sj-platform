package com.bwsw.sj.stubs.module.output

import java.util.Calendar

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.stubs.module.output.data.StubEsData

/**
 * Handler for work with t-stream envelopes
 * Executor trait for output-streaming module
 *
 * @author Kseniya Tomskikh
 */
class StubOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  override def onMessage(envelope: TStreamEnvelope): List[Envelope] = {
    val list = envelope.data.map { row =>
      val value = objectSerializer.deserialize(row).asInstanceOf[Int]

      val data: StubEsData = new StubEsData
      data.value = value
      data.testDate = Calendar.getInstance().getTime

      data
    }

    list
  }
}
