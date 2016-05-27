package com.bwsw.sj.module.output

import com.bwsw.sj.common.module.entities.TStreamEnvelope
import com.bwsw.sj.common.module.output.OutputStreamingHandler
import com.bwsw.sj.common.module.output.model.OutputEnvelope
import com.bwsw.sj.module.output.data.StubEsData

/**
  * Created: 27/05/16
  *
  * @author Kseniya Tomskikh
  */
class StubOutputHandler extends OutputStreamingHandler {

  def onTransaction(envelope: TStreamEnvelope): List[OutputEnvelope] = {
    val data = new StubEsData
    data.txn = envelope.txnUUID.toString
    val outputEnvelope = new OutputEnvelope
    outputEnvelope.data = data
    val list = List(outputEnvelope)
    list
  }

}
