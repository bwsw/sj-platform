package com.bwsw.sj.engine.core.output

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}

/**
 *
 * It is responsible for output module execution logic. Module uses a specific instance to personalize its work.
 * Executor provides the methods, which don't do anything by default so you should define their implementation by yourself.
 *
 * @author Kseniya Tomskikh
 */
class OutputStreamingExecutor extends StreamingExecutor {

  /**
   *
   */
  def onMessage(envelope: TStreamEnvelope): List[Envelope] = {
    List()
  }
}
