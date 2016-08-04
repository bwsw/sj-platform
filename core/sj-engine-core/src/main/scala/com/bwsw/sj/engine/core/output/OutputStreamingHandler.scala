package com.bwsw.sj.engine.core.output

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}

/**
  * Created: 26/05/2016
  *
  * @author Kseniya Tomskikh
  */
trait OutputStreamingHandler extends StreamingExecutor {

  /**
    *
    * @param envelope
    * @return
    */
  def onTransaction(envelope: TStreamEnvelope): List[OutputEnvelope]

}
