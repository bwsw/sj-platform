package com.bwsw.sj.examples.sflow.module.output

import java.util.Date

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.examples.sflow.module.output.data.StubData

/**
  * Created by diryavkin_dn on 13.01.17.
  */
class Executor extends OutputStreamingExecutor {
  val jsonSerializer = new JsonSerializer()
  val objectSerializer = new ObjectSerializer()

  /**
    * Transform t-stream transaction to output entities
    *
    * @param envelope Input T-Stream envelope
    * @return List of output envelopes
    */
  override def onMessage(envelope: TStreamEnvelope): List[Envelope] = {
    val list = envelope.data.map { row =>
      val value = objectSerializer.deserialize(row).asInstanceOf[collection.mutable.Map[Any, Any]]

      val dataJDBC: StubData = new StubData
      dataJDBC.reducedValue = value
      dataJDBC
    }
    list
  }
}
