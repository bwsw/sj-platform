package com.bwsw.sj.stubs.module.output

import java.util.Calendar

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.stubs.module.output.data.StubEsData

/**
  * Handler for work with t-stream envelopes
  * Executor trait for output-streaming module
  *
  * Created: 27/05/16
  *
  * @author Kseniya Tomskikh
  */
class StubOutputHandler extends OutputStreamingHandler {

  val objectSerializer = new ObjectSerializer()

  /**
    * Transform t-stream transaction to output entities
    *
    * @param envelope Input T-Stream envelope
    * @return List of output envelopes
    */
  def onTransaction(envelope: TStreamEnvelope): List[OutputEnvelope] = {
    val list = envelope.data.map { row =>
      val data: StubEsData = new StubEsData

      val value = objectSerializer.deserialize(row).asInstanceOf[Int]
      data.value = value
      data.testDate = Calendar.getInstance().getTime

      val outputEnvelope = new OutputEnvelope
      outputEnvelope.data = data
      outputEnvelope.streamType = "elasticsearch-output"
      outputEnvelope
    }
    list
  }

}
