package com.bwsw.sj.engine.core.output

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager

import scala.reflect.runtime.universe._

/**
  *
  * It is responsible for output module execution logic. Module uses a specific instance to personalize its work.
  * Executor provides the methods, which don't do anything by default so you should define their implementation by yourself.
  *
  * @author Kseniya Tomskikh
  */
class OutputStreamingExecutor[T: TypeTag](manager: OutputEnvironmentManager) extends StreamingExecutor {

  override def getType() = typeOf[T]
  /**
    * it is invoked for every received message from one of the inputs that are defined within the instance.
    * Inside the method you have an access to the message that has the TStreamEnvelope type.
    * By extension a t-stream envelope should be transformed to output envelopes.
    * The output envelope can have an elasticsearch type (this such of type has only one field - data
    * that has to be an OutputData data type,You should create a class implementing OutputData
    * and define such fields that will be in congruence with an ES index document structure)
    * or JDBC type.
    *
    */
  def onMessage(envelope: TStreamEnvelope): List[Envelope] = {
    List()
  }
}
