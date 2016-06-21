package com.bwsw.sj.module.output

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.module.output.data.StubEsData

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
      data.txn = envelope.txnUUID.toString
      data.value = objectSerializer.deserialize(row).asInstanceOf[Int]
      val outputEnvelope = new OutputEnvelope
      outputEnvelope.data = data
      outputEnvelope.streamType = "elasticsearch-output"
      outputEnvelope
    }
    list
  }

}
