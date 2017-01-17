package com.bwsw.sj.examples.sflow.module.output

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.examples.sflow.module.output.data.SrcAsData

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
    val list:List[Envelope] = List[Envelope]()
    envelope.data.foreach { row =>
      val value = objectSerializer.deserialize(row).asInstanceOf[collection.mutable.Map[Int, Int]]
      list ::: value.map(pair => new SrcAsData(pair._1, pair._2)).toList
    }
    list
  }
}
