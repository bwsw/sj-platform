package com.bwsw.sj.engine.core.output

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}

/**
  *
  *
  * @author Kseniya Tomskikh
  */
class OutputStreamingExecutor extends StreamingExecutor {
  
  def onMessage(envelope: TStreamEnvelope): List[OutputEnvelope] = {
    List()
  }
}
