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
   * it is invoked for every received message from one of the inputs that are defined within the instance.
   * Inside the method you have an access to the message that has the TStreamEnvelope type.
   * By extension a t-stream envelope is transformed to output envelopes (the t-stream envelope data contains a set of integers).
   * This output envelope is for output of elasticsearch type and this such of type has only one field - data that has to be an OutputData data type.
   * In the first section the integers are converted to Int type using a serializer,
   * which can be created the following way: val objectSerializer = new ObjectSerializer() .
   * In the second section is created a StubEsData containing two fields
   * but it's not necessary to be in precisely this way
   * because you should define such fields that will be in congruence with an ES index document structure.
   */
  def onMessage(envelope: TStreamEnvelope): List[Envelope] = {
    List()
  }
}
