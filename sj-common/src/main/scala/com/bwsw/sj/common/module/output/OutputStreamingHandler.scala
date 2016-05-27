package com.bwsw.sj.common.module.output

import com.bwsw.sj.common.module.entities.TStreamEnvelope
import com.bwsw.sj.common.module.output.model.OutputEnvelope

/**
  * Created: 26/05/2016
  *
  * @author Kseniya Tomskikh
  */
trait OutputStreamingHandler {

  /**
    *
    * @param envelope
    * @return
    */
  def onTransaction(envelope: TStreamEnvelope): List[OutputEnvelope]

}
