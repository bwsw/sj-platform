package com.bwsw.sj.stubs.module.output

import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.output.types.rest.{RestEntityBuilder, RestField}
import com.bwsw.sj.stubs.module.output.data.StubRestData

/**
  * @author Pavel Tomskikh
  */
class StubOutputExecutorRest(manager: OutputEnvironmentManager)
  extends OutputStreamingExecutor[(String, Seq[Int])](manager) {

  override def onMessage(envelope: TStreamEnvelope[(String, Seq[Int])]) = {
    println("Processed: " + envelope.data.size + " elements")

    val list = envelope.data.dequeueAll(_ => true).map {
      case (stringValue, seqValue) =>
        val data = new StubRestData
        data.stringValue = stringValue
        data.seqValue = seqValue

        data
    }

    list
  }

  override def getOutputEntity = {
    val entityBuilder = new RestEntityBuilder()
    val entity = entityBuilder
      .field(new RestField("stringValue"))
      .field(new RestField("seqValue"))
      .build()

    entity
  }
}
