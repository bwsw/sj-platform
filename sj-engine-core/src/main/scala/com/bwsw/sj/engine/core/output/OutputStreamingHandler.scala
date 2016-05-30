package com.bwsw.sj.engine.core.output

import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}

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
